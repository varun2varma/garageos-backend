"""Permanent security/isolation regression suite. Every entry here is a
confirmed, previously-real vulnerability or authorization gap — never
weaken or remove one of these to make a release "pass."
"""
from conftest import steps_for


def test_vehicle_idor_cross_customer_edit_forbidden(journey):
    rec, ctx = journey
    step = next((s for s in rec.steps if "Vehicle IDOR" in s.name), None)
    assert step is not None, "Vehicle IDOR regression did not run"
    assert step.status == "PASS", f"VEHICLE IDOR REGRESSED — cross-customer vehicle edit is exploitable again: {step.detail}"


def test_customer_payment_endpoint_still_returns_403(journey):
    rec, ctx = journey
    step = next((s for s in rec.steps if "Customer payment endpoint correctly returns 403" in s.name), None)
    assert step is not None, "Customer-payment 403 regression did not run"
    assert step.status == "PASS", f"Customer payment authorization regressed: {step.detail}"


def test_technician_cannot_see_unassigned_jobcard(journey):
    rec, ctx = journey
    step = next((s for s in rec.steps if "Technician does not see job before assignment" in s.name), None)
    assert step is not None, "Technician pre-assignment visibility regression did not run"
    assert step.status == "PASS", f"Technician can see unassigned work — information leak: {step.detail}"


# --- Known gaps, listed honestly rather than silently absent ---

def test_TODO_cross_garage_isolation_not_yet_automated():
    """A second, fully independent garage + a customer/technician/manager
    scoped to it, proving Garage A's job cards/vehicles/media are
    unreachable by Garage B's users, is NOT YET implemented — see
    MASTER_E2E_COVERAGE.md 'Next incremental test to add'. Recorded as an
    explicit TODO test rather than left undocumented.
    """
    import pytest
    pytest.skip("NOT_YET_AUTOMATED: cross-garage isolation — see MASTER_E2E_COVERAGE.md")
