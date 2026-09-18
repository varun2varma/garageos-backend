from conftest import steps_for


def test_phase_07_has_no_unexpected_failures(journey):
    rec, ctx = journey
    steps = steps_for(rec, "07_invoice_and_payment")
    assert steps, "Phase 07 recorded no steps"
    unexpected = [s for s in steps if s.status == "FAIL"]
    assert not unexpected, "Phase 07 had unexpected FAILs:\n" + "\n".join(f"- {s.name}: {s.detail}" for s in unexpected)


def test_view_invoice_navigation_regression(journey):
    """Permanent backend-side regression for the customer 'View invoice'
    wrong-screen bug (fixed in Flutter this session) — the backend contract
    this screen depends on (GET /customer/invoices with correct amount and
    PENDING status) must keep working."""
    rec, ctx = journey
    step = next((s for s in rec.steps if "Customer sees invoice" in s.name), None)
    assert step is not None
    assert step.status == "PASS", f"Customer invoice visibility regressed: {step.detail}"


def test_customer_payment_release_blocker_is_still_blocked(journey):
    """This intentionally does NOT assert success. Per explicit instruction:
    do not bypass this with a manager call and call it passing. If this
    endpoint's authorization is ever redesigned to permit CUSTOMER (with an
    ownership check), update this test to assert 200 + ownership at that
    point — do not just flip the expected status blindly.
    """
    rec, ctx = journey
    blocked = next((s for s in rec.steps if s.name == "Customer Pay Now (real path)"), None)
    assert blocked is not None, "Customer payment blocker step did not run"
    assert blocked.status == "BLOCKED", (
        f"Customer payment step status changed to {blocked.status} — if this now succeeds, "
        f"the release blocker may have been fixed; update this test deliberately, don't just "
        f"make it pass. Detail: {blocked.detail}"
    )
