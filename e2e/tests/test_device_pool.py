"""Unit tests for master_e2e.device_pool.DevicePool — pure allocation logic,
no physical hardware or backend required. Every serial used below is an
obviously-fake, arbitrary string precisely to prove the allocator contains
no special-casing of any specific real device (no hardcoded serials).
"""
from __future__ import annotations

import pytest

from master_e2e.device_pool import Device, DevicePool, DevicePoolError, parse_adb_devices


# ---------------------------------------------------------------------------
# adb output parsing
# ---------------------------------------------------------------------------

def test_parse_adb_devices_keeps_only_state_device():
    output = """List of devices attached
192.168.1.58:34697     device product:e3qxins model:SM_S928B device:e3q transport_id:2
emulator-5554   offline
ABCDEF123456    unauthorized
192.168.1.99:9999      device product:x model:y device:z transport_id:9

"""
    assert parse_adb_devices(output) == ["192.168.1.58:34697", "192.168.1.99:9999"]


def test_parse_adb_devices_empty_output():
    assert parse_adb_devices("List of devices attached\n\n") == []


# ---------------------------------------------------------------------------
# Mode 1 — one device, multiple persona transitions
# ---------------------------------------------------------------------------

def test_single_device_cycles_through_all_personas():
    pool = DevicePool([Device(serial="dev-A")])

    d1 = pool.acquire_persona("CUSTOMER")
    assert d1.serial == "dev-A"
    assert d1.current_persona == "CUSTOMER"

    d2 = pool.acquire_persona("MANAGER")
    assert d2.serial == "dev-A", "the only device must be reused, not fabricated"
    assert d2.current_persona == "MANAGER"

    d3 = pool.acquire_persona("TECHNICIAN")
    assert d3.serial == "dev-A"

    d4 = pool.acquire_persona("DRIVER")
    assert d4.serial == "dev-A"

    actions = [e.action for e in pool.timeline]
    assert actions[0] == "Login"
    assert actions[1:] == ["Logout+Login", "Logout+Login", "Logout+Login"], (
        "every persona switch on one device must be a real logout/login transition"
    )


def test_single_device_timeline_step_numbers_increment():
    pool = DevicePool([Device(serial="dev-A")])
    pool.acquire_persona("CUSTOMER")
    pool.acquire_persona("MANAGER")
    assert [e.step for e in pool.timeline] == [1, 2]


# ---------------------------------------------------------------------------
# Reuse rules
# ---------------------------------------------------------------------------

def test_already_authenticated_persona_is_reused_without_logout_login():
    pool = DevicePool([Device(serial="dev-A"), Device(serial="dev-B")])
    pool.acquire_persona("CUSTOMER")  # -> dev-A (Login)
    pool.acquire_persona("MANAGER")  # -> dev-B (Login)

    d = pool.acquire_persona("CUSTOMER")
    assert d.serial == "dev-A"
    assert pool.timeline[-1].action == "Reuse", (
        "a device already logged in as the requested persona must be reused as-is, "
        "never logged out and back in"
    )


def test_reuse_prefers_already_authenticated_device_over_a_free_idle_one():
    # dev-A is CUSTOMER, dev-B is idle (never used). Re-acquiring CUSTOMER
    # must reuse dev-A, not consume the idle dev-B.
    pool = DevicePool([Device(serial="dev-A"), Device(serial="dev-B")])
    pool.acquire_persona("CUSTOMER")
    d = pool.acquire_persona("CUSTOMER")
    assert d.serial == "dev-A"
    assert d.current_persona == "CUSTOMER"
    assert pool.devices["dev-B"].current_persona is None, "idle device must remain untouched"


# ---------------------------------------------------------------------------
# Mode 2/3/4 — multiple devices, intelligent allocation
# ---------------------------------------------------------------------------

def test_two_devices_allocate_distinct_personas_independently():
    pool = DevicePool([Device(serial="dev-A"), Device(serial="dev-B")])
    cust = pool.acquire_persona("CUSTOMER")
    mgr = pool.acquire_persona("MANAGER")
    assert {cust.serial, mgr.serial} == {"dev-A", "dev-B"}


def test_four_devices_one_persona_each_no_reuse_needed():
    pool = DevicePool([Device(serial=f"dev-{i}") for i in range(4)])
    assignments = {
        p: pool.acquire_persona(p).serial
        for p in ("CUSTOMER", "MANAGER", "TECHNICIAN", "DRIVER")
    }
    assert len(set(assignments.values())) == 4, "four personas on four devices must not collide"
    actions = [e.action for e in pool.timeline]
    assert actions == ["Login", "Login", "Login", "Login"]


def test_extra_idle_devices_are_never_touched():
    pool = DevicePool([Device(serial=f"dev-{i}") for i in range(9)])
    for p in ("CUSTOMER", "MANAGER", "TECHNICIAN", "DRIVER"):
        pool.acquire_persona(p)
    used = {e.device for e in pool.timeline}
    assert len(used) == 4, "only 4 of the 9 connected devices should ever be allocated"
    idle = [d for d in pool.devices.values() if d.current_persona is None]
    assert len(idle) == 5


def test_more_devices_than_needed_still_uses_one_canonical_allocation_path():
    # Same assertions regardless of whether 1, 4, or 9 devices are connected
    # — proves device count affects only allocation, never the persona set.
    for n in (1, 2, 4, 9):
        pool = DevicePool([Device(serial=f"dev-{i}") for i in range(n)])
        personas_seen = []
        for p in ("CUSTOMER", "MANAGER", "TECHNICIAN", "DRIVER"):
            d = pool.acquire_persona(p)
            personas_seen.append(d.current_persona)
        assert personas_seen == ["CUSTOMER", "MANAGER", "TECHNICIAN", "DRIVER"]


# ---------------------------------------------------------------------------
# Concurrency
# ---------------------------------------------------------------------------

def test_concurrent_reservation_blocks_reuse_by_a_different_operation():
    pool = DevicePool([Device(serial="dev-A"), Device(serial="dev-B")])
    cust = pool.acquire_persona("CUSTOMER")
    drv = pool.acquire_persona("DRIVER", exclude=frozenset({cust.serial}))
    assert cust.serial != drv.serial, "a genuinely concurrent scenario needs two distinct devices"

    pool.reserve_concurrent([cust.serial, drv.serial], tag="nav_live")

    with pytest.raises(DevicePoolError):
        pool.reserve_concurrent([cust.serial], tag="unrelated_op")


def test_reserved_device_is_invisible_to_acquisition_until_released():
    pool = DevicePool([Device(serial="dev-A"), Device(serial="dev-B")])
    cust = pool.acquire_persona("CUSTOMER")
    drv = pool.acquire_persona("DRIVER", exclude=frozenset({cust.serial}))
    pool.reserve_concurrent([cust.serial, drv.serial], tag="nav_live")

    # No device left to acquire TECHNICIAN on while both are reserved.
    with pytest.raises(DevicePoolError):
        pool.acquire_persona("TECHNICIAN")

    pool.release_concurrent([cust.serial, drv.serial], tag="nav_live")

    tech = pool.acquire_persona("TECHNICIAN")
    assert tech.serial in {"dev-A", "dev-B"}, "once released, either device may be reused"


def test_release_clears_reservation_and_allows_reallocation():
    pool = DevicePool([Device(serial="dev-A")])
    d = pool.acquire_persona("CUSTOMER")
    pool.reserve_concurrent([d.serial], tag="op1")
    pool.release(d.serial)
    assert pool.devices[d.serial].reserved_by is None
    # Persona identity is untouched by release() — only the reservation is cleared.
    assert pool.devices[d.serial].current_persona == "CUSTOMER"


# ---------------------------------------------------------------------------
# Failure handling — must raise real, reportable errors, never fabricate
# ---------------------------------------------------------------------------

def test_missing_required_device_raises_when_pool_is_empty():
    pool = DevicePool([])
    with pytest.raises(DevicePoolError):
        pool.acquire_persona("CUSTOMER")


def test_missing_required_device_when_all_devices_reserved():
    pool = DevicePool([Device(serial="dev-A")])
    d = pool.acquire_persona("CUSTOMER")
    pool.reserve_concurrent([d.serial], tag="busy")
    with pytest.raises(DevicePoolError):
        pool.acquire_persona("MANAGER")


def test_disconnected_device_is_never_reallocated():
    pool = DevicePool([Device(serial="dev-A"), Device(serial="dev-B")])
    pool.acquire_persona("CUSTOMER")  # -> dev-A
    pool.disconnect("dev-A")

    mgr = pool.acquire_persona("MANAGER")
    assert mgr.serial == "dev-B", "a disconnected device must never be handed out again"

    with pytest.raises(DevicePoolError):
        # Both remaining logins already used dev-B; dev-A is gone, so a
        # third distinct concurrent device genuinely does not exist.
        pool.acquire_persona("TECHNICIAN", exclude=frozenset({"dev-B"}))


def test_disconnect_of_unknown_serial_raises_not_silently_ignored():
    pool = DevicePool([Device(serial="dev-A")])
    with pytest.raises(DevicePoolError):
        pool.disconnect("dev-does-not-exist")


# ---------------------------------------------------------------------------
# No hardcoded serials — the allocator must treat any serial identically
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("serials", [
    ["192.168.1.58:34697"],
    ["emulator-5554"],
    ["completely-made-up-serial-xyz"],
    ["Z1Y2X3W4V5"],
])
def test_allocator_is_serial_agnostic(serials):
    pool = DevicePool([Device(serial=s) for s in serials])
    d = pool.acquire_persona("CUSTOMER")
    assert d.serial == serials[0]


# ---------------------------------------------------------------------------
# plan_allocation — the function the orchestrator actually calls
# ---------------------------------------------------------------------------

_JOURNEY_PERSONAS = ["CUSTOMER", "MANAGER", "TECHNICIAN", "DRIVER"]


def test_plan_allocation_single_device_puts_every_persona_on_it():
    pool = DevicePool([Device(serial="dev-A")])
    plan = pool.plan_allocation(_JOURNEY_PERSONAS)
    assert plan == {"dev-A": _JOURNEY_PERSONAS}


def test_plan_allocation_four_devices_one_persona_each():
    pool = DevicePool([Device(serial=f"dev-{i}") for i in range(4)])
    plan = pool.plan_allocation(_JOURNEY_PERSONAS)
    assert len(plan) == 4
    assert {p for personas in plan.values() for p in personas} == set(_JOURNEY_PERSONAS)
    assert all(len(personas) == 1 for personas in plan.values())


def test_plan_allocation_nine_devices_only_uses_four():
    pool = DevicePool([Device(serial=f"dev-{i}") for i in range(9)])
    plan = pool.plan_allocation(_JOURNEY_PERSONAS)
    assert len(plan) == 4, "5 of the 9 connected devices must remain unallocated/idle"


def test_plan_allocation_two_devices_reuses_for_the_extra_personas():
    pool = DevicePool([Device(serial="dev-A"), Device(serial="dev-B")])
    plan = pool.plan_allocation(_JOURNEY_PERSONAS)
    assert len(plan) == 2
    assert sum(len(v) for v in plan.values()) == 4
    assert set(plan.keys()) == {"dev-A", "dev-B"}


def test_timeline_markdown_matches_reporting_spec_shape():
    pool = DevicePool([Device(serial="dev-A"), Device(serial="dev-B")])
    pool.acquire_persona("CUSTOMER")
    pool.acquire_persona("MANAGER")
    md = pool.render_timeline_markdown()
    assert "| Step | Persona | Device | Action |" in md
    assert "| 1 | CUSTOMER | dev-A | Login |" in md
    assert "| 2 | MANAGER | dev-B | Login |" in md
