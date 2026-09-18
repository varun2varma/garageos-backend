"""Dynamic multi-device persona allocator for the Android Suite (Suite B).

Extends the Master Release Test with the CORE RULE requested: the number of
connected physical Android devices must never be a requirement for the
business journey. There is exactly ONE canonical Master Journey; this module
is the allocator that sits underneath it, deciding which physical device
runs which persona's UI actions at any given step — never a second journey
implementation per device count.

    MasterJourney
        |
    PersonaStep
        |
    DevicePool.acquire_persona(persona)
        |
    Physical Android device (adb serial)
        |
    Flutter integration_test actions (master_journey_test.dart)
        |
    backend assertions (master_e2e/assertions.py)

No device serial is ever hardcoded here — every serial this module operates
on comes from `adb devices` output (real runs) or is passed in by a caller
(unit tests use obviously-fake serials like "emulator-fake-1" precisely to
prove the allocator never special-cases a specific device).
"""
from __future__ import annotations

import re
import subprocess
from dataclasses import dataclass, field
from datetime import datetime


class DevicePoolError(Exception):
    """Raised when the pool genuinely cannot satisfy a request — e.g. no
    connected device is available for a persona, or a concurrent scenario
    needs more dedicated devices than are currently connected. This is a
    real, reportable BLOCKED condition, never silently swallowed."""


@dataclass
class Device:
    serial: str
    connected: bool = True
    current_persona: str | None = None
    app_installed: bool = False
    app_running: bool = False
    session_state: str = "unknown"  # "logged_out" | "logged_in" | "unknown"
    reserved_by: str | None = None  # concurrent-operation reservation tag, or None


@dataclass
class Persona:
    name: str
    credentials: dict
    assigned_device: str | None = None


@dataclass
class AllocationEvent:
    step: int
    persona: str
    device: str
    action: str  # "Login" | "Reuse" | "Logout+Login" | "Release" | "Reserve" | "Disconnect"
    timestamp: datetime = field(default_factory=datetime.now)


_ADB_DEVICES_LINE = re.compile(r"^(\S+)\s+(\w+)")


def parse_adb_devices(output: str) -> list[str]:
    """Parses `adb devices -l` (or plain `adb devices`) output, returning
    only serials whose state is exactly `device` — offline/unauthorized/
    no-permissions entries are real, reportable disconnection states, not
    devices this pool may use."""
    serials: list[str] = []
    for line in output.splitlines():
        line = line.strip()
        if not line or line.startswith("List of devices"):
            continue
        m = _ADB_DEVICES_LINE.match(line)
        if not m:
            continue
        serial, state = m.group(1), m.group(2)
        if state == "device":
            serials.append(serial)
    return serials


class DevicePool:
    def __init__(self, devices: list[Device] | None = None):
        self.devices: dict[str, Device] = {d.serial: d for d in (devices or [])}
        self.timeline: list[AllocationEvent] = []
        self._step = 0

    @classmethod
    def discover(cls, adb_path: str = "adb") -> "DevicePool":
        """Real discovery: shells out to `adb devices -l`. Never assumes a
        fixed device count or a specific serial — this is the only place a
        real ADB invocation happens; everything else in this module is
        pure allocation logic, independently unit-testable without any
        physical hardware."""
        result = subprocess.run(
            [adb_path, "devices", "-l"], capture_output=True, text=True, timeout=15
        )
        serials = parse_adb_devices(result.stdout)
        return cls([Device(serial=s, connected=True) for s in serials])

    # ---- introspection -----------------------------------------------

    def connected_devices(self) -> list[Device]:
        return [d for d in self.devices.values() if d.connected]

    def count(self) -> int:
        return len(self.connected_devices())

    # ---- allocation -----------------------------------------------------

    def acquire_persona(self, persona_name: str, exclude: frozenset[str] = frozenset()) -> Device:
        """Implements the acquisition rules exactly as specified, in
        priority order:

        1. If a connected, unreserved device is already logged in as this
           persona, reuse it as-is (no logout/login round trip).
        2. Otherwise prefer a connected, unreserved, IDLE device (one that
           has never been logged into anything yet) — this is what makes
           Mode 4+ ("Customer -> A, Manager -> B, Technician -> C, Driver
           -> D") keep distinct personas on distinct devices when enough
           devices exist, and what keeps extra idle devices untouched.
        3. Only if no idle device exists, reuse a connected, unreserved
           device that currently holds a DIFFERENT persona, by logging it
           out and back in as this one. This is the single-device fallback
           (Mode 1): with only one device, there is never an idle
           candidate after the first login, so every later persona
           deliberately switches on that same device.
        4. Never touches a device with an active `reserved_by` tag — that
           device is "currently required for a concurrent activity" per
           rule 3, and is invisible to acquisition until released.
        5. Raises DevicePoolError (a real, reportable BLOCKED condition,
           never silently downgraded) if no device is available at all —
           this is the exact signal a caller uses to fall back to
           sequential logout/login or to the backend/STOMP harness instead
           of fabricating a second physical session that doesn't exist.
        """
        candidates = [
            d for d in self.devices.values()
            if d.connected and d.reserved_by is None and d.serial not in exclude
        ]

        for d in candidates:
            if d.current_persona == persona_name:
                self._record(persona_name, d.serial, "Reuse")
                return d

        idle = [d for d in candidates if d.current_persona is None]
        if idle:
            d = idle[0]
            d.current_persona = persona_name
            d.session_state = "logged_in"
            self._record(persona_name, d.serial, "Login")
            return d

        for d in candidates:
            d.current_persona = persona_name
            d.session_state = "logged_in"
            self._record(persona_name, d.serial, "Logout+Login")
            return d

        raise DevicePoolError(
            f"No available device for persona '{persona_name}': "
            f"{len(self.connected_devices())} connected, "
            f"{len(exclude)} excluded, "
            f"{sum(1 for d in self.devices.values() if d.reserved_by)} reserved for a concurrent operation."
        )

    def release(self, serial: str) -> None:
        """Marks a device safe to reuse for the next persona step. Does
        NOT log it out — the canonical journey logs out explicitly as its
        own step (matching the real UI action a persona performs); this
        only clears any leftover concurrent-reservation tag."""
        d = self._require(serial)
        d.reserved_by = None
        self._record(d.current_persona or "-", serial, "Release")

    def reserve_concurrent(self, serials: list[str], tag: str) -> None:
        """Dedicates the given devices to one concurrent operation (e.g.
        live Driver GPS + Customer observation) for its duration. A device
        already reserved under a DIFFERENT tag cannot be double-booked —
        that is rule 3 enforced at reservation time, not just at
        acquisition time."""
        for serial in serials:
            d = self._require(serial)
            if d.reserved_by is not None and d.reserved_by != tag:
                raise DevicePoolError(
                    f"Device {serial} is already reserved for concurrent operation '{d.reserved_by}'; "
                    f"cannot also reserve it for '{tag}'."
                )
        for serial in serials:
            d = self._require(serial)
            d.reserved_by = tag
            self._record(d.current_persona or "-", serial, "Reserve")

    def release_concurrent(self, serials: list[str], tag: str) -> None:
        for serial in serials:
            d = self._require(serial)
            if d.reserved_by == tag:
                d.reserved_by = None
                self._record(d.current_persona or "-", serial, "Release")

    def disconnect(self, serial: str) -> None:
        """Simulates/records a device going offline mid-run. A disconnected
        device is never a candidate for acquisition again until it
        reappears in a fresh `discover()`."""
        d = self._require(serial)
        d.connected = False
        d.current_persona = None
        d.reserved_by = None
        self._record("-", serial, "Disconnect")

    # ---- internals --------------------------------------------------

    def _require(self, serial: str) -> Device:
        if serial not in self.devices:
            raise DevicePoolError(f"Unknown device serial: {serial!r}")
        return self.devices[serial]

    def _record(self, persona: str, serial: str, action: str) -> None:
        self._step += 1
        self.timeline.append(AllocationEvent(self._step, persona, serial, action))

    def plan_allocation(self, personas: list[str]) -> dict[str, list[str]]:
        """Runs `acquire_persona` for each persona in order (exactly the
        sequence the canonical Master Journey needs them in) and returns
        the resulting {serial: [persona, ...]} grouping — i.e. which
        physical device ends up responsible for which persona(s). This is
        the same allocation ONE journey uses regardless of device count:
        with 1 device every persona lands in that device's list (a real
        logout/login cycle); with 4+ devices each persona typically gets
        its own device; with 9 devices the extra 5 simply never appear as
        a key. Does not itself run anything on a device — it is the
        decision a caller (e.g. run_suite_b_devices.py) turns into actual
        `flutter test -d <serial>` invocations, one per resulting device.
        """
        plan: dict[str, list[str]] = {}
        for persona in personas:
            device = self.acquire_persona(persona)
            plan.setdefault(device.serial, []).append(persona)
        return plan

    # ---- reporting --------------------------------------------------

    def timeline_rows(self) -> list[dict]:
        return [
            {"step": e.step, "persona": e.persona, "device": e.device, "action": e.action,
             "timestamp": e.timestamp.isoformat()}
            for e in self.timeline
        ]

    def render_timeline_markdown(self) -> str:
        lines = [
            "| Step | Persona | Device | Action |",
            "|---|---|---|---|",
        ]
        for e in self.timeline:
            lines.append(f"| {e.step} | {e.persona} | {e.device} | {e.action} |")
        return "\n".join(lines)
