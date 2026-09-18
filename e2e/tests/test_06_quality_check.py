from conftest import steps_for


def test_phase_06_has_no_unexpected_failures(journey):
    rec, ctx = journey
    steps = steps_for(rec, "06_quality_check")
    assert steps, "Phase 06 recorded no steps"
    failed = [s for s in steps if s.status == "FAIL"]
    assert not failed, "Phase 06 had unexpected FAILs:\n" + "\n".join(f"- {s.name}: {s.detail}" for s in failed)


def test_customer_progress_consistent_after_qc_regression(journey):
    """Permanent regression for the confirmed-and-fixed bug where
    CustomerPortalServiceImpl.trackRepair() hardcoded qualityChecked=false
    forever (MASTER_E2E_COVERAGE.md #5)."""
    rec, ctx = journey
    step = next((s for s in rec.steps if "Customer progress view consistent after QC" in s.name), None)
    assert step is not None
    assert step.status == "PASS", f"Customer QC-progress regression reappeared: {step.detail}"
