from conftest import steps_for


def test_phase_03_has_no_unexpected_failures(journey):
    rec, ctx = journey
    steps = steps_for(rec, "03_inspection_and_media")
    assert steps, "Phase 03 recorded no steps"
    failed = [s for s in steps if s.status == "FAIL"]
    assert not failed, "Phase 03 had unexpected FAILs:\n" + "\n".join(f"- {s.name}: {s.detail}" for s in failed)


def test_inspection_media_drive_upload_or_known_blocker(journey):
    rec, ctx = journey
    step = next((s for s in rec.steps if "Upload inspection photo" in s.name), None)
    assert step is not None, "Inspection media upload step did not run"
    assert step.status in ("PASS", "BLOCKED"), f"Unexpected status: {step.status} — {step.detail}"
    if step.status == "BLOCKED":
        import pytest
        pytest.skip(f"Known external blocker (see MASTER_E2E_COVERAGE.md #4): {step.detail}")
