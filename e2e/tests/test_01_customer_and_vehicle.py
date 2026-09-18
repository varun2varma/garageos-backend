"""Thin pytest wrapper over Phase 01 of the ONE shared Master Release Test
journey (see conftest.py::journey). Does not re-run the journey — asserts
on the StepResults it already recorded, so `pytest e2e/tests` and
`python run.py` report the identical facts.
"""
from conftest import steps_for


def test_phase_01_has_no_unexpected_failures(journey):
    rec, ctx = journey
    steps = steps_for(rec, "01_customer_and_vehicle")
    assert steps, "Phase 01 recorded no steps at all — journey fixture did not run"
    failed = [s for s in steps if s.status == "FAIL"]
    assert not failed, "Phase 01 had unexpected FAILs:\n" + "\n".join(f"- {s.name}: {s.detail}" for s in failed)


def test_vehicle_idor_regression_specifically(journey):
    rec, ctx = journey
    idor = next((s for s in rec.steps if "IDOR" in s.name), None)
    assert idor is not None, "Vehicle IDOR regression step did not run"
    assert idor.status == "PASS", f"Vehicle IDOR regression did not pass: {idor.detail}"
