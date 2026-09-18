import pytest

from conftest import steps_for


def test_phase_09_closure_has_no_unexpected_failures(journey):
    rec, ctx = journey
    steps = steps_for(rec, "09_closure")
    assert steps, "Phase 09 recorded no steps"
    unexpected = [
        s for s in steps
        if s.status == "FAIL" and "no longer sees active work" not in s.name
    ]
    assert not unexpected, "Phase 09 had unexpected FAILs:\n" + "\n".join(f"- {s.name}: {s.detail}" for s in unexpected)


def test_job_card_reaches_closed(journey):
    rec, ctx = journey
    step = next((s for s in rec.steps if s.name.startswith("Close Job Card")), None)
    assert step is not None, "Close Job Card step did not run"
    assert step.status == "PASS", f"Job Card did not reach CLOSED: {step.detail}"
    assert ctx.jobcard_number, "No jobcard_number on context"
