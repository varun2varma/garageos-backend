#!/usr/bin/env python3
"""GarageST Master Release Test — dynamic multi-device Android orchestrator.

Discovers every physical Android device currently connected via `adb
devices`, allocates the canonical journey's 4 working personas (CUSTOMER,
MANAGER, TECHNICIAN, DRIVER) across them via DevicePool.plan_allocation()
(reusing a device across personas via logout/login when there are fewer
devices than personas, spreading personas across distinct devices when
there are enough), then runs ONE `flutter test integration_test/
master_journey_test.dart -d <serial>` process per device that ends up in
the plan.

Devices run SEQUENTIALLY, not concurrently — confirmed via a real 2-device
run that Gradle's project-level build lock serializes concurrent `flutter
test` invocations against this one project directory regardless (the
second device's process blocks silently waiting for the lock, which looks
identical to "never started" without per-device streaming). Each device's
real progress is independently, continuously observable via a per-device
log file plus milestone detection (build started / APK built / installing
/ test started), and independently verified via `adb` before (device
state) and after (app actually installed) — never inferred merely from a
subprocess object existing.

There is exactly one business journey (`master_journey_test.dart`'s
persona-cycle group); this script only decides, and reports, which
physical device runs which slice of it. Device count changes execution
allocation only — never the journey itself.

Usage:
    python run_suite_b_devices.py --run-context reports/MRT....run_context.json
    python run_suite_b_devices.py --run-context reports/MRT....run_context.json --dev-host 192.168.1.24:8080
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from datetime import datetime

sys.path.insert(0, ".")

# This script forwards raw lines from Flutter's own console output (which
# includes non-ASCII glyphs like the "√" build-success checkmark)
# straight into its own print() calls. On Windows, stdout defaults to the
# system codepage (cp1252) when not attached to a UTF-8-aware terminal,
# which cannot encode those glyphs and crashes the whole run mid-device —
# confirmed live: this took down device 1's run right after a real,
# successful Gradle build, losing all its progress. Reconfiguring stdout
# to UTF-8 with lossy replacement (Python 3.7+) makes every print() in
# this file safe regardless of what Flutter prints, not just the one that
# happened to crash first.
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.stderr.reconfigure(encoding="utf-8", errors="replace")

from master_e2e.client import MasterE2EClient
from master_e2e.device_pool import Device, DevicePool, DevicePoolError

JOURNEY_PERSONAS = ["CUSTOMER", "MANAGER", "TECHNICIAN", "DRIVER"]


def find_adb(explicit: str | None) -> str:
    if explicit:
        return explicit
    local_appdata = os.environ.get("LOCALAPPDATA", "")
    candidate = os.path.join(local_appdata, "Android", "Sdk", "platform-tools", "adb.exe")
    if local_appdata and os.path.isfile(candidate):
        return candidate
    return "adb"  # fall back to PATH


def find_flutter(explicit: str | None) -> str:
    """On Windows, `flutter` is a .bat/.cmd shim; subprocess.run's default
    (shell=False) CreateProcess call does not resolve that the way a real
    shell's PATH lookup would, so the bare name fails with WinError 2. Use
    shutil.which (which DOES do the .PATHEXT-aware lookup a shell does) to
    find the real executable path once, rather than hardcoding one."""
    if explicit:
        return explicit
    import shutil
    found = shutil.which("flutter")
    if found:
        return found
    return "flutter"


def fetch_job_card_status(base_url: str, manager_username: str, password: str, job_card_number: str) -> str:
    """Best-effort, real live read of the job card's current
    ServiceWorkflow status — not the status captured at the end of Suite
    A's run, which may be stale if anything happened since. Failure here
    is reported honestly, never papered over with a guessed status."""
    try:
        client = MasterE2EClient(base_url=base_url)
        body = client.post("/auth/login", {"username": manager_username, "password": password}, actor="MANAGER")
        token = body["accessToken"]
        status_body = client.get(f"/workflow/{job_card_number}/status", token=token, actor="MANAGER")
        return status_body.get("status") or status_body.get("workflowStatus") or str(status_body)
    except Exception as e:  # noqa: BLE001
        return f"UNKNOWN (live status fetch failed: {type(e).__name__}: {e})"


def verify_ui_created_job_card(base_url: str, manager_username: str, password: str, job_card_number: str,
                                expected_mobile: str, expected_registration: str) -> dict:
    """Backend-side confirmation that the Job Card the Manager UI actually
    created via '+Workflow' is real and belongs to the same customer and
    vehicle the Customer UI's booking used — the additional assertion the
    mission explicitly allows alongside (never instead of) exercising the
    real UI. A UI 'PASS' with no Job Card actually existing, or belonging
    to the wrong customer/vehicle, is exactly the false-green this checks
    for."""
    try:
        client = MasterE2EClient(base_url=base_url)
        body = client.post("/auth/login", {"username": manager_username, "password": password}, actor="MANAGER")
        token = body["accessToken"]
        import urllib.parse as _urlparse
        query = _urlparse.urlencode({"jobCardNumber": job_card_number})
        job = client.get(f"/jobcards/search?{query}", token=token, actor="MANAGER")
        mobile_ok = job.get("customerMobileNumber") == expected_mobile
        vehicle_ok = job.get("registrationNumber") == expected_registration
        return {
            "found": True,
            "jobCardNumber": job.get("jobCardNumber"),
            "status": job.get("status"),
            "customerMobileNumber": job.get("customerMobileNumber"),
            "registrationNumber": job.get("registrationNumber"),
            "customerMatches": mobile_ok,
            "vehicleMatches": vehicle_ok,
            "verified": mobile_ok and vehicle_ok,
        }
    except Exception as e:  # noqa: BLE001
        return {"found": False, "error": f"{type(e).__name__}: {e}", "verified": False}


APP_PACKAGE = "com.garagest.app"

# Recognized as milestones in flutter test's own console output, matched
# against each streamed line to build a real, observable per-device
# timeline (connected -> build started -> APK built -> installed ->
# launched -> test started -> result) instead of trusting that a
# subprocess existing means the device is actually doing anything.
# NOTE: "loading <file>" is printed immediately, before Gradle even
# starts (confirmed live: it appeared at +2.5s, 22s before build_started)
# — it means "about to compile the test file", not "app launched". The
# real proof that install+launch succeeded is (setUpAll) starting, which
# only happens once the app is actually running on the device.
_MILESTONE_PATTERNS = [
    ("test_file_loading", "loading "),
    ("build_started", "Running Gradle task"),
    ("apk_built", "Built build"),
    ("installing", "Installing build"),
    ("app_launched", "(setUpAll)"),
]


def adb_get_state(adb_path: str, serial: str) -> str:
    try:
        result = subprocess.run([adb_path, "-s", serial, "get-state"], capture_output=True,
                                 text=True, encoding="utf-8", errors="replace", timeout=10)
        return result.stdout.strip() or f"(no output, rc={result.returncode})"
    except Exception as e:  # noqa: BLE001
        return f"ERROR: {type(e).__name__}: {e}"


def adb_app_installed(adb_path: str, serial: str) -> bool:
    try:
        result = subprocess.run([adb_path, "-s", serial, "shell", "pm", "list", "packages", APP_PACKAGE],
                                 capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=15)
        return APP_PACKAGE in result.stdout
    except Exception:  # noqa: BLE001
        return False


def run_flutter_test_on_device(flutter_exe: str, flutter_repo: str, serial: str, dev_host: str,
                                dart_defines: dict[str, str], adb_path: str, log_path: str) -> dict:
    """Runs ONE flutter test invocation for ONE device, streaming its
    output to `log_path` line-by-line AS IT HAPPENS (not fully buffered
    until exit) so this device's real progress is independently observable
    — confirmed necessary: an earlier concurrent-Popen version of this
    orchestrator produced zero output until process exit, which is exactly
    what let a genuinely stuck second device go undetected for minutes."""
    args = [flutter_exe, "test", "integration_test/master_journey_test.dart", "-d", serial]
    for key, value in dart_defines.items():
        args.append(f"--dart-define={key}={value}")
    args.append(f"--dart-define=GarageST_DEV_HOST=http://{dev_host}")

    state_before = adb_get_state(adb_path, serial)
    print(f"  [{serial}] adb state before: {state_before}")

    milestones: dict[str, bool] = {name: False for name, _ in _MILESTONE_PATTERNS}
    start = datetime.now()
    lines: list[str] = []
    installed_live: bool | None = None  # verified WHILE the app is actually running, not after test-runner cleanup
    ui_created_job_card_number: str | None = None

    with open(log_path, "w", encoding="utf-8") as log_file:
        proc = subprocess.Popen(args, cwd=flutter_repo, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                 text=True, encoding="utf-8", errors="replace", bufsize=1)
        for line in proc.stdout:
            lines.append(line)
            log_file.write(line)
            log_file.flush()
            if "MRT_UI_CREATED_JOB_CARD_NUMBER:" in line:
                ui_created_job_card_number = line.split("MRT_UI_CREATED_JOB_CARD_NUMBER:", 1)[1].strip()
                print(f"  [{serial}] UI created a real Job Card: {ui_created_job_card_number}")
            for name, pattern in _MILESTONE_PATTERNS:
                if not milestones[name] and pattern in line:
                    milestones[name] = True
                    elapsed = (datetime.now() - start).total_seconds()
                    print(f"  [{serial}] +{elapsed:.1f}s milestone: {name} ({line.strip()[:100]})")
                    if name == "app_launched":
                        # This is the only point at which "is it actually
                        # installed" is answerable by adb: `flutter test`
                        # uninstalls the app as part of its own cleanup
                        # once the process exits, so checking afterward
                        # (confirmed live: produced a false "NOT FOUND" on
                        # a run that had just passed) always reports
                        # not-installed regardless of whether the run
                        # succeeded. Checking here, while the process is
                        # still mid-run, is the real, honest verification.
                        installed_live = adb_app_installed(adb_path, serial)
                        print(f"  [{serial}] adb pm list packages (live, mid-run): "
                              f"{'INSTALLED' if installed_live else 'NOT FOUND'} ({APP_PACKAGE})")
        proc.wait(timeout=900)

    duration = (datetime.now() - start).total_seconds()
    if installed_live is None:
        # app_launched never fired -> the run never got far enough to
        # install+launch; report that plainly rather than a guess.
        print(f"  [{serial}] app never reached the launched state this run "
              f"(no '(setUpAll)' milestone observed) — treating as NOT installed/launched.")
        installed_live = False

    full_output = "".join(lines)
    return {
        "serial": serial,
        "command": " ".join(args),
        "returncode": proc.returncode,
        "durationSeconds": round(duration, 1),
        "adbStateBefore": state_before,
        "appInstalledLive": installed_live,
        "uiCreatedJobCardNumber": ui_created_job_card_number,
        "milestones": milestones,
        "stdoutTail": full_output[-4000:],
        "stderrTail": "",
        "logFile": log_path,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-context", required=True, help="path to the *.run_context.json Suite A wrote")
    parser.add_argument("--flutter-repo", default=os.path.join("..", "..", "garagest_flutter"))
    parser.add_argument("--dev-host", default="192.168.1.24:8080")
    parser.add_argument("--base-url", default="http://localhost:8080/api/v1")
    parser.add_argument("--adb-path", default=None)
    parser.add_argument("--flutter-path", default=None)
    parser.add_argument("--out-dir", default="reports")
    parser.add_argument("--dry-run", action="store_true",
                         help="build the allocation plan and per-device commands but do not execute flutter test "
                              "(used for devices beyond what's physically connected, or for pure static validation)")
    args = parser.parse_args()

    with open(args.run_context, encoding="utf-8") as f:
        run_context = json.load(f)

    adb_path = find_adb(args.adb_path)
    flutter_path = find_flutter(args.flutter_path)
    pool = DevicePool.discover(adb_path=adb_path)
    detected = pool.connected_devices()

    print("=== GarageST Master Release Test — Suite B Device Orchestrator ===")
    print(f"Run ID (Suite A context): {run_context['runId']}")
    print(f"Job Card: {run_context['jobCardNumber']}")
    print(f"adb: {adb_path}")
    print(f"flutter: {flutter_path}")
    print(f"Detected devices: {len(detected)}")
    for i, d in enumerate(detected, start=1):
        print(f"  [{i}] {d.serial}")

    if not detected:
        print("\nENVIRONMENT FAILURE: no Android device in 'adb devices' state 'device'. "
              "Connect at least one device (wireless or USB ADB) before running Suite B.")
        return 2

    try:
        plan = pool.plan_allocation(JOURNEY_PERSONAS)
    except DevicePoolError as e:
        print(f"\nENVIRONMENT FAILURE: DevicePool could not allocate the journey's personas: {e}")
        return 2

    print("\nDevice Pool:")
    for serial, personas in plan.items():
        print(f"  {', '.join(personas):<28} -> {serial}")

    print("\nDevice allocation timeline:")
    print(pool.render_timeline_markdown())

    device_switches = sum(1 for e in pool.timeline if e.action == "Logout+Login")
    logins = sum(1 for e in pool.timeline if e.action in ("Login", "Logout+Login"))
    reuses = sum(1 for e in pool.timeline if e.action == "Reuse")

    # SEQUENTIAL by design, even with 2+ devices — confirmed via a real
    # run (2026-09-18, 2 physical devices) that launching `flutter test`
    # concurrently against this same project directory deadlocks: Gradle
    # takes an exclusive project-level lock (confirmed present at
    # garagest_flutter/android/.gradle/**/*.lock) for the whole
    # build+install step, so a second concurrent invocation blocks
    # silently — indistinguishable from "not started" without per-device
    # streaming, which is exactly what went undetected before this fix.
    # Running one device fully to completion before starting the next
    # avoids the lock entirely; Gradle's own build cache means each
    # device after the first is typically faster (native/Kotlin build
    # outputs are reused — only the dart-define-dependent kernel snapshot
    # and the final install step redo work). True concurrent devices
    # remain possible for a scenario that specifically needs simultaneity
    # (see reserve_concurrent/release_concurrent in device_pool.py) — that
    # is a distinct requirement from routine persona-cycling, which does
    # not need two devices running at the same instant.
    device_results = []
    for serial, personas in plan.items():
        dart_defines = {
            "MRT_RUN_ID": run_context["runId"],
            "MRT_PASSWORD": run_context["password"],
            "MRT_CUSTOMER_USERNAME": run_context["personas"]["CUSTOMER"],
            "MRT_MANAGER_USERNAME": run_context["personas"]["MANAGER"],
            "MRT_TECHNICIAN_USERNAME": run_context["personas"]["TECHNICIAN"],
            "MRT_DRIVER_USERNAME": run_context["personas"]["DRIVER"],
            "MRT_DEVICE_PERSONAS": ",".join(personas),
            # Optional — only present in run_context.json from a Suite A
            # run new enough to write them. Their absence (empty string)
            # is exactly what gates the booking->+Workflow UI journey
            # group off in master_journey_test.dart, so an older context
            # file degrades gracefully instead of crashing.
            "MRT_CUSTOMER_MOBILE": run_context.get("customerMobile", ""),
            "MRT_VEHICLE_REGISTRATION": run_context.get("vehicleRegistration", ""),
            "MRT_GARAGE_NAME": run_context.get("garageName", ""),
        }
        if args.dry_run:
            cmd = [flutter_path, "test", "integration_test/master_journey_test.dart", "-d", serial]
            for k, v in dart_defines.items():
                cmd.append(f"--dart-define={k}={v}")
            cmd.append(f"--dart-define=GarageST_DEV_HOST=http://{args.dev_host}")
            print(f"\n[DRY RUN] Would execute on {serial}: personas={personas}")
            print(f"  {' '.join(cmd)}")
            device_results.append({"serial": serial, "personas": personas, "executed": False, "command": " ".join(cmd)})
            continue

        print(f"\n--- Executing on {serial} (personas: {', '.join(personas)}) ---")
        log_path = os.path.join(args.out_dir, f"{run_context['runId']}.suite_b.{serial.replace(':', '_')}.log")
        result = run_flutter_test_on_device(
            flutter_path, args.flutter_repo, serial, args.dev_host, dart_defines, adb_path, log_path
        )
        result["personas"] = personas
        result["executed"] = True
        device_results.append(result)
        print(f"  [{serial}] exit code: {result['returncode']} ({result['durationSeconds']}s) — log: {log_path}")

    disconnected = [s for s, d in pool.devices.items() if not d.connected]
    live_status = fetch_job_card_status(
        args.base_url, run_context["personas"]["MANAGER"], run_context["password"], run_context["jobCardNumber"]
    )

    executed = [r for r in device_results if r.get("executed")]
    failed = [r for r in executed if r.get("returncode") != 0]

    # Backend-side confirmation for every UI-created Job Card this run
    # (the booking -> "+Workflow" journey group can run on more than one
    # device in a multi-device plan, each creating its own) — verifies it
    # really exists and belongs to the same customer/vehicle the Customer
    # UI's booking used, per the mission's "backend assertion in addition
    # to, never instead of, the UI action" rule.
    ui_job_card_verifications = []
    for r in executed:
        number = r.get("uiCreatedJobCardNumber")
        if not number:
            continue
        verification = verify_ui_created_job_card(
            args.base_url, run_context["personas"]["MANAGER"], run_context["password"],
            number, run_context.get("customerMobile", ""), run_context.get("vehicleRegistration", "")
        )
        verification["serial"] = r["serial"]
        ui_job_card_verifications.append(verification)
        print(f"  [{r['serial']}] UI-created Job Card {number} backend verification: "
              f"{'VERIFIED (same customer+vehicle)' if verification.get('verified') else 'NOT VERIFIED — ' + str(verification)}")

    print("\n=== SUITE B DEVICE ORCHESTRATION SUMMARY ===")
    print(f"Detected devices: {len(detected)}")
    print(f"Personas used: {', '.join(JOURNEY_PERSONAS)}")
    print(f"Devices actually allocated: {len(plan)}")
    print(f"Login/logout transitions (device switches, same-device persona changes): {device_switches}")
    print(f"Same-persona reuses (no logout/login needed): {reuses}")
    print(f"Total logins: {logins}")
    print(f"Concurrent device reservations this run: 0 (no live-navigation concurrent scenario requested)")
    print(f"Failed/disconnected devices: {disconnected or 'none'}")
    print(f"Final Job Card: {run_context['jobCardNumber']}")
    print(f"Final Job Card status (live-fetched): {live_status}")
    print(f"Device processes executed: {len(executed)} | failed: {len(failed)}")
    if ui_job_card_verifications:
        verified_count = sum(1 for v in ui_job_card_verifications if v.get("verified"))
        print(f"UI-created Job Cards this run: {len(ui_job_card_verifications)} "
              f"(backend-verified same customer+vehicle: {verified_count})")

    report = {
        "runId": run_context["runId"],
        "jobCardNumber": run_context["jobCardNumber"],
        "finalJobCardStatus": live_status,
        "uiJobCardVerifications": ui_job_card_verifications,
        "detectedDevices": [d.serial for d in detected],
        "allocationPlan": plan,
        "allocationTimeline": pool.timeline_rows(),
        "deviceSwitches": device_switches,
        "reuses": reuses,
        "totalLogins": logins,
        "disconnectedDevices": disconnected,
        "deviceResults": device_results,
        "generatedAt": datetime.now().isoformat(),
    }
    os.makedirs(args.out_dir, exist_ok=True)
    report_path = os.path.join(args.out_dir, f"{run_context['runId']}.suite_b_devices.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2, default=str)

    md_lines = [
        f"# GarageST Master Release Test — Suite B Device Orchestration — {run_context['runId']}",
        "",
        f"Detected devices: {len(detected)}",
        "",
        "## Device allocation timeline",
        "",
        pool.render_timeline_markdown(),
        "",
        "## Summary",
        "",
        f"- Personas used: {', '.join(JOURNEY_PERSONAS)}",
        f"- Devices actually allocated: {len(plan)}",
        f"- Device switches (logout/login on a reused device): {device_switches}",
        f"- Same-persona reuses: {reuses}",
        f"- Total logins: {logins}",
        f"- Concurrent device reservations this run: 0",
        f"- Failed/disconnected devices: {disconnected or 'none'}",
        f"- Final Job Card: {run_context['jobCardNumber']}",
        f"- Final Job Card status (live-fetched): {live_status}",
        f"- Device processes executed: {len(executed)} | failed: {len(failed)}",
        "",
        "## Per-device results",
        "",
        "| Device | Personas | adb state before | Build started | APK built | Installing | App launched | "
        "App installed (adb-verified, mid-run) | Exit code | Duration (s) |",
        "|---|---|---|---|---|---|---|---|---|---|",
    ]
    for r in device_results:
        m = r.get("milestones", {})
        md_lines.append(
            f"| {r['serial']} | {', '.join(r['personas'])} | {r.get('adbStateBefore', '-')} | "
            f"{m.get('build_started', '-')} | {m.get('apk_built', '-')} | {m.get('installing', '-')} | "
            f"{m.get('app_launched', '-')} | {r.get('appInstalledLive', '-')} | "
            f"{r.get('returncode', '-')} | {r.get('durationSeconds', '-')} |"
        )
    md_path = os.path.join(args.out_dir, f"{run_context['runId']}.suite_b_devices.md")
    with open(md_path, "w", encoding="utf-8") as f:
        f.write("\n".join(md_lines) + "\n")

    print(f"\nReports written: {report_path}, {md_path}")

    if not executed:
        return 0  # dry-run only, nothing to fail
    unverified_job_cards = [v for v in ui_job_card_verifications if not v.get("verified")]
    if unverified_job_cards:
        print(f"\nFAIL: {len(unverified_job_cards)} UI-created Job Card(s) failed backend verification "
              f"(a green Flutter test result alone is not proof — see uiJobCardVerifications in the report).")
    return 1 if (failed or unverified_job_cards) else 0


if __name__ == "__main__":
    raise SystemExit(main())
