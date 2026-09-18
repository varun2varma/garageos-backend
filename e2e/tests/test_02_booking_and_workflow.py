from conftest import steps_for


def test_phase_02_has_no_unexpected_failures(journey):
    rec, ctx = journey
    steps = steps_for(rec, "02_booking_and_workflow")
    assert steps, "Phase 02 recorded no steps"
    failed = [s for s in steps if s.status == "FAIL"]
    assert not failed, "Phase 02 had unexpected FAILs:\n" + "\n".join(f"- {s.name}: {s.detail}" for s in failed)


def test_plus_workflow_button_creates_real_jobcard(journey):
    rec, ctx = journey
    step = next((s for s in rec.steps if "+Workflow" in s.name), None)
    assert step is not None, "+Workflow step did not run"
    assert step.status == "PASS", f"+Workflow did not create a correctly-linked Job Card: {step.detail}"
    assert ctx.jobcard_number, "No jobcard_number captured after +Workflow"
