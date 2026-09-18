#!/usr/bin/env python3
"""GarageST Master Release Test — backend suite (Suite A) runner.

Usage:
    python run.py                 # full journey
    python run.py --phase 5       # phases 1..5 only (still sequential from 1)
    python run.py --base-url http://localhost:8080/api/v1

Requires a real, already-running backend (see ../garagest-e2e.env for the
Postgres/Google Drive config it needs — this script never starts the
backend itself, matching "verify readiness, don't own the environment").
"""
from __future__ import annotations

import argparse
import sys
import urllib.error
import urllib.request

sys.path.insert(0, ".")

from master_e2e.client import MasterE2EClient
from master_e2e.fixtures import PASSWORD, RunFixtures
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

PHASES = [
    (1, "Customer & Vehicle", phase_01_customer_and_vehicle),
    (2, "Booking & +Workflow", phase_02_booking_and_workflow),
    (3, "Inspection & Media", phase_03_inspection_and_media),
    (4, "Estimate & Approval", phase_04_estimate_and_approval),
    (5, "Repair Task Lifecycle", phase_05_repair_tasks),
    (6, "Quality Check", phase_06_quality_check),
    (7, "Invoice & Payment", phase_07_invoice_and_payment),
    (8, "Driver & Navigation", phase_08_driver_and_navigation),
    (9, "Closure", phase_09_closure),
]


def check_backend_reachable(base_url: str) -> bool:
    ping_url = base_url.replace("/api/v1", "") + "/actuator/health"
    try:
        with urllib.request.urlopen(ping_url, timeout=3) as resp:
            return resp.status < 500
    except Exception:
        # Actuator may not be exposed; fall back to any response at all
        # from a known real endpoint (401/403 still proves the server is up).
        try:
            urllib.request.urlopen(base_url + "/master/employee-roles", timeout=3)
            return True
        except urllib.error.HTTPError:
            return True  # server answered with an HTTP status -> it's up
        except Exception:
            return False


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080/api/v1")
    parser.add_argument("--phase", type=int, default=9, help="run phases 1..N (default: all)")
    parser.add_argument("--out-dir", default="reports")
    args = parser.parse_args()

    if not check_backend_reachable(args.base_url):
        print(f"ENVIRONMENT FAILURE: backend not reachable at {args.base_url}. "
              f"Start it (see ../garagest-e2e.env) before running the Master Release Test.")
        return 2

    client = MasterE2EClient(base_url=args.base_url)
    fixtures = RunFixtures()
    ctx = JourneyContext(client=client, fixtures=fixtures)
    ctx.make_personas()

    rec = StepRecorder(run_id=fixtures.run_id, client=client)

    print(f"=== GarageST Master Release Test — Backend Suite (Suite A) ===")
    print(f"Run ID: {fixtures.run_id}")
    print(f"Base URL: {args.base_url}")
    print(f"Running phases 1..{args.phase}\n")

    for number, name, fn in PHASES:
        if number > args.phase:
            break
        print(f"\n--- Phase {number:02d}: {name} ---")
        try:
            fn(rec, ctx)
        except Exception as e:  # noqa: BLE001 — a phase-level crash (outside any rec.step) must not kill the whole run/report
            rec.step(f"{number:02d}_{name}", "PHASE CRASHED (unhandled exception outside step boundaries)",
                      "RUNNER", lambda e=e: (_ for _ in ()).throw(e))

    paths = rec.write(args.out_dir)
    counts = rec.summary_counts()

    # Shared run-context handoff for Suite B (Android). Suite B must operate
    # on THIS SAME job card and THESE SAME real accounts, on whatever
    # physical device(s) DevicePool assigns them to — never re-register its
    # own independent data. This is the one artifact that makes "Customer on
    # Device A / Manager on Device B ... all operate on the SAME Job Card"
    # true across the API/UI boundary, not just an assertion in prose.
    run_context = {
        "runId": fixtures.run_id,
        "password": PASSWORD,
        "jobCardNumber": ctx.jobcard_number,
        "personas": {
            "CUSTOMER": ctx.customer.seed.username,
            "MANAGER": ctx.manager.seed.username,
            "TECHNICIAN": ctx.technician.seed.username,
            "DRIVER": ctx.driver.seed.username,
        },
        # For Suite B's UI-driven booking -> +Workflow journey: the SAME
        # real customer/vehicle/garage Suite A already created via API, so
        # a Flutter-UI-created booking and Job Card belong to a real,
        # already-onboarded customer rather than fabricated data.
        "customerMobile": ctx.fixtures.customer.mobile,
        # The vehicle's registration is EDITED during phase_01 ("Edit
        # vehicle (own)") from vehicle_registration to
        # vehicle_registration_edited — that edit is the real, current
        # value in the database by the time this run context is written,
        # so Suite B's search-by-registration must use this one, not the
        # original pre-edit value.
        "vehicleRegistration": ctx.fixtures.vehicle_registration_edited,
        "garageName": ctx.fixtures.garage_name,
    }
    import json as _json
    import os as _os
    run_context_path = _os.path.join(args.out_dir, f"{fixtures.run_id}.run_context.json")
    with open(run_context_path, "w", encoding="utf-8") as f:
        _json.dump(run_context, f, indent=2)

    print("\n=== SUMMARY ===")
    print(f"PASS={counts['PASS']} FAIL={counts['FAIL']} BLOCKED={counts['BLOCKED']} NOT_YET_AUTOMATED={counts['NOT_YET_AUTOMATED']}")
    print(f"Job Card: {ctx.jobcard_number}")
    print(f"Reports written:")
    for kind, p in paths.items():
        print(f"  {kind}: {p}")
    print(f"  runContext: {run_context_path}")

    return 1 if counts["FAIL"] > 0 else 0


if __name__ == "__main__":
    raise SystemExit(main())
