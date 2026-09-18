# GarageST — Running the Master Release Test locally

This document is written so another developer can run the full Master
Release Test **without Claude or Claude Code**. Everything the suite needs
lives in this repository (`garageos-backend`) and its sibling
`garagest_flutter` checkout — the only things you provide yourself are the
prerequisites below, your own credentials/devices, and a running backend.

See also: [MASTER_E2E_COVERAGE.md](MASTER_E2E_COVERAGE.md) — the living
coverage matrix, known issues, and architecture notes this document
complements.

---

## 1. Prerequisites

| Tool | Version (tested) | Check |
|---|---|---|
| Java | 17 | `java -version` |
| Maven wrapper | bundled (`mvnw`/`mvnw.cmd`) | `./mvnw.cmd -v` |
| PostgreSQL | any recent version, running locally | `psql --version` |
| Python | 3.10+ | `python --version` |
| Flutter SDK | stable channel | `flutter --version` |
| Android SDK platform-tools (`adb`) | any recent | `adb version` |
| PowerShell | 5.1+ (Windows) | built in |

Install Python test dependencies once:

```bash
cd garageos-backend/e2e
pip install -r requirements.txt
```

`requirements.txt` is intentionally small: `websocket-client` (real STOMP
frames over a raw WebSocket) and `pytest` (the wrapper layer around the
same journey `run.py` drives directly). The rest of Suite A is Python
standard library only (`urllib`) — no HTTP client dependency to keep in
sync with the backend's own stack.

## 2. Backend environment

1. Have a local Postgres instance reachable and a `garageos` database
   created (or matching whatever `DB_NAME` you set).
2. Copy the template and fill in real values:
   ```bash
   cp garagest-e2e.env.example garagest-e2e.env
   ```
   Edit `garagest-e2e.env` with your local Postgres credentials and a real
   Google Cloud OAuth client (Drive API scope) — see §3. **Never commit
   `garagest-e2e.env`** (it's gitignored) and never paste its contents
   anywhere shareable.
3. Start the backend with those variables exported:
   ```bash
   # bash
   set -a && source garagest-e2e.env && set +a && ./mvnw.cmd spring-boot:run
   ```
   ```powershell
   # PowerShell
   Get-Content garagest-e2e.env | ForEach-Object {
       if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
       $k, $v = $_.Split('=', 2)
       [Environment]::SetEnvironmentVariable($k, $v)
   }
   .\mvnw.cmd spring-boot:run
   ```
   Wait for `Started GarageOsApplication` before running any suite —
   `master-release-test.ps1` checks readiness itself but does not start
   the backend for you (on purpose: it should never silently paper over an
   environment that isn't actually up).

## 3. Google Drive OAuth setup (for media coverage)

Suite A's media-upload steps (inspection photos, per-task repair evidence)
exercise the real Google Drive integration, not a mock. You need:

1. A Google Cloud project with the **Drive API** enabled.
2. An OAuth 2.0 Client ID (type: Web application), with the redirect URI
   matching `GOOGLE_DRIVE_REDIRECT_URI` in your `garagest-e2e.env` exactly.
3. A Drive folder to root everything under — put its file ID in
   `GOOGLE_DRIVE_ROOT_FOLDER_ID`.
4. Complete the OAuth consent flow once (via the backend's own
   `/media/google/*` endpoints or admin UI, however your local setup
   authorizes it) so `GoogleDriveCredentialDataStore` has a persisted
   refresh token in Postgres.

If this isn't set up, media steps correctly report **BLOCKED** (not a
silent skip, not a fake pass) — see MASTER_E2E_COVERAGE.md's Known Issue
#4 for the specific failure mode this currently surfaces even with OAuth
configured (an unresolved token-refresh gap).

## 4. Android device setup

Either USB or **wireless ADB** works; the suite discovers whatever is
connected, it never assumes a specific serial.

**Wireless ADB (recommended — matches how this suite was developed):**
```bash
adb tcpip 5555          # once, over USB, to enable wireless mode
adb connect <device-ip>:<port>
adb devices              # confirm state "device", not "unauthorized"/"offline"
```

Repeat for as many physical devices as you want to use. The suite works
identically with 1 device (persona cycling via logout/login) or several
(personas spread across devices) — see §6.

**APK install/launch:** you do not need to build or install the APK
yourself. `flutter test integration_test/master_journey_test.dart -d
<serial>` builds, installs, and launches the app on that device as part of
running the test — exactly like `flutter run` would.

## 5. Commands

All from the `garageos-backend` repo root:

```powershell
.\master-release-test.ps1 -Full        # Suite A (all 9 phases) + Suite B
.\master-release-test.ps1 -Backend     # Suite A only
.\master-release-test.ps1 -Android     # Suite B only (needs a prior -Backend run's context)
.\master-release-test.ps1 -Media       # Suite A, phases 1-5 (through Google Drive)
.\master-release-test.ps1 -Navigation  # Suite A, phases 1-8 (through STOMP/dummy-GPS)
.\master-release-test.ps1 -Security    # Suite A, security regressions only
```

Equivalent direct commands, if you'd rather not go through the PowerShell
wrapper (useful on macOS/Linux, or for iterating on one suite):

```bash
cd e2e
python run.py --base-url http://localhost:8080/api/v1        # Suite A
python run_suite_b_devices.py --run-context reports/<runId>.run_context.json \
    --flutter-repo ../../garagest_flutter --dev-host <your-LAN-ip>:8080   # Suite B
python -m pytest tests/test_device_pool.py -v                # DevicePool unit tests (no hardware needed)
python -m pytest tests/test_99_security_regressions.py -v    # security regressions only
```

`-Android`/`run_suite_b_devices.py` always operates on the **most recent**
`e2e/reports/*.run_context.json` — the real accounts and Job Card a Suite A
run just created. Suite B never registers its own accounts or Job Card; run
`-Backend` (or `-Full`) at least once before `-Android` alone.

## 6. One-device vs. multi-device mode

There is exactly **one** canonical business journey. Device count only
changes *execution allocation* — see MASTER_E2E_COVERAGE.md's "Dynamic
multi-device persona orchestration" section for the full allocator design
(`DevicePool.acquire_persona`/`plan_allocation` in `e2e/master_e2e/
device_pool.py`).

- **1 device connected:** all 4 personas (CUSTOMER, MANAGER, TECHNICIAN,
  DRIVER) cycle on it via real logout/login, in one `flutter test`
  process.
- **2+ devices connected:** `plan_allocation` spreads personas across idle
  devices first, falling back to logout/login reuse only when devices run
  out; `run_suite_b_devices.py` launches one `flutter test -d <serial>`
  subprocess per device the plan actually uses, in parallel
  (`subprocess.Popen`, real OS-level concurrency — not a simulated loop).
- **More devices than personas (5, 8-9+):** the extra devices are simply
  never allocated; nothing about the journey changes.

The exact allocation is runtime-driven and reported in the run's
`*.suite_b_devices.md`/`.json` (a "Device allocation timeline" table:
step, persona, device, action).

## 7. Concurrent scenarios (e.g. live Driver + Customer navigation)

`DevicePool.reserve_concurrent([...], tag)` / `release_concurrent(...)`
dedicate specific devices to one concurrent operation for its duration — a
reserved device is invisible to `acquire_persona` (never logged out) until
released. This is implemented and unit-tested (`test_device_pool.py`), but
a live 2-device concurrent Driver-GPS + Customer-observation UI flow needs
the customer-side live-tracking screen to exist in the Flutter app first —
see MASTER_E2E_COVERAGE.md for the current, honest status of that gap.
With only one device connected, genuinely simultaneous mobile sessions are
not possible — use Suite A's existing backend/STOMP harness
(`test_09_navigation_and_dummy_gps.py`) for protocol-level concurrency
coverage instead of claiming two simulated phones.

## 8. Reports

Every Suite A run writes three files plus a run-context handoff to
`e2e/reports/`:

- `<runId>.json` — machine-readable, includes the full API transcript
- `<runId>.html` — human-readable, color-coded by status
- `<runId>.md` — Markdown summary (this is what gets pasted into PRs/chat)
- `<runId>.run_context.json` — the shared handoff Suite B consumes (never
  a credential file — just IDs and the fixed test password)

A Suite B device-orchestration run additionally writes:

- `<runId>.suite_b_devices.json` / `.md` — device allocation timeline,
  per-device pass/fail, live-fetched final Job Card status

**These are all generated artifacts, gitignored by design** (see
`.gitignore`'s "Master Release Test (e2e/)" section) — do not hand-edit or
commit them. If you need to preserve one as a baseline, copy it somewhere
outside `e2e/reports/` or `git add -f` it deliberately.

## 9. Exit codes

| Exit code | Meaning |
|---|---|
| 0 | All steps PASS (or BLOCKED/NOT_YET_AUTOMATED only — no FAIL) |
| 1 | At least one step FAILed (a real, confirmed defect — see the report) |
| 2 | ENVIRONMENT FAILURE — backend unreachable, no device connected, Flutter repo not found, or no run context available for Suite B |

`master-release-test.ps1`'s own exit code is the worst of whichever suites
you ran (`-Full` fails overall if either Suite A or Suite B fails).

## 10. Troubleshooting

- **`ENVIRONMENT FAILURE: backend not reachable`** — start the backend
  first (§2); the script deliberately never starts it for you.
- **`ENVIRONMENT FAILURE: no Android device found`** — `adb devices` must
  show at least one line ending in `device` (not `unauthorized`/`offline`).
  Re-pair with `adb connect` if a wireless device dropped.
- **`No Suite A run context found`** — run `-Backend` (or `-Full`) at
  least once; `-Android` alone has nothing to operate on otherwise.
- **`GarageOsApplicationTests.contextLoads` fails under `./mvnw.cmd test`**
  — almost always means `garagest-e2e.env` wasn't sourced into that shell
  before running Maven (the `${DB_HOST}` etc. placeholders in
  `application.properties` are then passed to the JDBC driver literally,
  unresolved). Re-source it in the same shell invocation as the test run.
- **Suite A media steps BLOCKED with `MEDIA_DRIVE_AUTH_FAILED`** — the
  stored Google Drive OAuth token has expired/been revoked; re-run the
  OAuth consent flow (§3). This is a real, currently-open gap — see
  MASTER_E2E_COVERAGE.md Known Issue #4 if it recurs even right after
  re-authorizing.
- **`flutter test` fails immediately with a device-not-found error even
  though `adb devices` shows it** — make sure the SAME device identity is
  visible to `flutter devices` too (`flutter devices` must list it); a
  freshly wireless-paired device sometimes needs `flutter devices` run
  once to be picked up by the Flutter tool's own device cache.
- **A run's `contextLoads`-style failure only reproduces via this script,
  never when you run Maven/Flutter directly** — check you're not running
  two backend instances on the same port (8080) simultaneously; the
  script does not detect or prevent that.
- **A multi-device run's second (or later) device never seems to start —
  no build/install output, `adb`'s foreground-activity check still shows
  the device's home/settings screen** — this is a real, confirmed Gradle
  limitation, not a hang: two `flutter test` invocations against the same
  project directory contend on Gradle's own project-level build lock
  (`garagest_flutter/android/.gradle/**/*.lock`), and the second one
  blocks silently until the first fully finishes. `run_suite_b_devices.py`
  therefore runs devices **sequentially**, one fully to completion before
  the next starts — this is expected and by design, not a bug to work
  around by parallelizing further. Confirmed live: a 2-device run
  completed with both devices genuinely installing, launching, and
  passing independently, at roughly 4-5 minutes per device, run one after
  the other.
- **A device's install step takes far longer than another device's in the
  same run (e.g. 70s+ vs 15-20s)** — this is real, observed WiFi/hardware
  variance between physical devices over wireless ADB, not a stuck
  process. Verify independently with `adb -s <serial> shell dumpsys
  activity activities | grep topResumedActivity` — if it's still showing
  a non-GarageST foreground activity after several minutes with zero
  growth in the relevant `dart`/`java` process's CPU time, that's a real
  stall worth investigating; steady low-but-nonzero CPU and an eventual
  state change is just a slow but working install.
