import pytest

from conftest import steps_for


def test_phase_05_repair_progresses_via_repair_task_endpoints(journey):
    """The core RepairTask lifecycle (assign/start/complete -> ServiceWorkflow)
    must always pass — this is the ServiceWorkflow-recognised path."""
    rec, ctx = journey
    steps = steps_for(rec, "05_repair_task_lifecycle")
    assert steps, "Phase 05 recorded no steps"
    hard_failures = [
        s for s in steps
        if s.status == "FAIL" and "JobAssignment/RepairTask divergence" not in s.name
        and "Customer/Manager/Owner see repair completion" not in s.name
    ]
    assert not hard_failures, "Phase 05 had unexpected FAILs:\n" + "\n".join(f"- {s.name}: {s.detail}" for s in hard_failures)


def test_jobassignment_repairtask_divergence_regression(journey):
    """PERMANENT regression guard for the disconnect discovered in the
    real-device session (2026-09-18): a technician's own UI completion
    (JobAssignment) must actually be reflected in RepairTask/ServiceWorkflow.
    XFAIL, not silently skipped — this is a known, open, HIGH-severity bug
    (see MASTER_E2E_COVERAGE.md #1), not something this suite should stop
    checking for just because it's already known.
    """
    rec, ctx = journey
    step = next((s for s in rec.steps if "Technician still sees active work" in s.detail or "no longer sees active work" in s.name), None)
    if step is None:
        pytest.skip("Closure-phase technician-visibility step did not run this session")
    if step.status == "FAIL":
        pytest.xfail(f"Known open bug (MASTER_E2E_COVERAGE.md #1): {step.detail}")
