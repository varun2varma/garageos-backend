"""Shared pytest fixtures for the Master Release Test.

The 9 phases share ONE JourneyContext, run once per pytest session, in
strict order — matching "ONE coherent business scenario, ONE Job Card"
rather than pytest's usual independent-test-isolation default. Individual
test_XX_*.py files assert on specific StepResults recorded during that one
run; they do not re-run the journey themselves.
"""
from __future__ import annotations

import os

import pytest

from master_e2e.client import MasterE2EClient
from master_e2e.fixtures import RunFixtures
from master_e2e.journey import (
    JourneyContext,
    phase_01_customer_and_vehicle,
    phase_02_booking_and_workflow,
    phase_03_inspection_and_media,
    phase_04_estimate_and_approval,
    phase_05_repair_tasks,
    phase_06_quality_check,
    phase_07_invoice_and_payment,
    phase_08_driver_and_navigation,
    phase_09_closure,
)
from master_e2e.report import StepRecorder

BASE_URL = os.environ.get("MRT_BASE_URL", "http://localhost:8080/api/v1")

_ALL_PHASES = [
    phase_01_customer_and_vehicle, phase_02_booking_and_workflow,
    phase_03_inspection_and_media, phase_04_estimate_and_approval,
    phase_05_repair_tasks, phase_06_quality_check,
    phase_07_invoice_and_payment, phase_08_driver_and_navigation,
    phase_09_closure,
]


@pytest.fixture(scope="session")
def journey():
    """Runs the entire 9-phase journey exactly once for the whole pytest
    session, and hands every test the same (rec, ctx) pair to assert
    against. A phase-level crash outside any rec.step is caught here too,
    same as run.py, so one bad phase doesn't prevent the report from being
    written or later phases' tests from at least reporting "no data."
    """
    client = MasterE2EClient(base_url=BASE_URL)
    fx = RunFixtures()
    ctx = JourneyContext(client=client, fixtures=fx)
    ctx.make_personas()
    rec = StepRecorder(run_id=fx.run_id, client=client)

    for phase_fn in _ALL_PHASES:
        try:
            phase_fn(rec, ctx)
        except Exception as e:  # noqa: BLE001
            rec.step(phase_fn.__name__, "PHASE CRASHED", "RUNNER", lambda e=e: (_ for _ in ()).throw(e))

    rec.write(os.path.join(os.path.dirname(__file__), "reports"))
    return rec, ctx


def steps_for(rec, phase_substring: str):
    return [s for s in rec.steps if phase_substring in s.phase]
