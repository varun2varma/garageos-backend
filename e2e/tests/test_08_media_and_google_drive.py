import pytest

from conftest import steps_for


def test_task_wise_media_association_or_known_blocker(journey):
    """Real mechanism: JobCardMedia.repairTaskId (no per-task Drive
    subfolder exists — see MASTER_E2E_COVERAGE.md #7)."""
    rec, ctx = journey
    step = next((s for s in rec.steps if "Upload per-task repair evidence" in s.name), None)
    assert step is not None, "Task-wise media step did not run"
    assert step.status in ("PASS", "BLOCKED"), f"Unexpected status: {step.status} — {step.detail}"
    if step.status == "BLOCKED":
        pytest.skip(f"Known external blocker (MASTER_E2E_COVERAGE.md #4): {step.detail}")


def test_media_survives_job_card_closure_or_known_blocker(journey):
    rec, ctx = journey
    step = next((s for s in rec.steps if "still retrievable after CLOSED" in s.name), None)
    assert step is not None, "Post-closure media step did not run"
    assert step.status in ("PASS", "BLOCKED"), f"Unexpected status: {step.status} — {step.detail}"
    if step.status == "BLOCKED":
        pytest.skip(f"Known external blocker (MASTER_E2E_COVERAGE.md #4): {step.detail}")
