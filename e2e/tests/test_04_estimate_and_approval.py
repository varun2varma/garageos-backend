from conftest import steps_for


def test_phase_04_has_no_unexpected_failures(journey):
    rec, ctx = journey
    steps = steps_for(rec, "04_estimate_and_approval")
    assert steps, "Phase 04 recorded no steps"
    failed = [s for s in steps if s.status == "FAIL"]
    assert not failed, "Phase 04 had unexpected FAILs:\n" + "\n".join(f"- {s.name}: {s.detail}" for s in failed)


def test_customer_approval_regression(journey):
    """Permanent regression for the historical 'PUT /estimates/{id} 400' /
    estimate-approval API failures observed before this suite existed."""
    rec, ctx = journey
    step = next((s for s in rec.steps if "Customer sees estimate and approves" in s.name), None)
    assert step is not None
    assert step.status == "PASS", f"Customer estimate approval regressed: {step.detail}"
