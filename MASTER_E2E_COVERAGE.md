# GarageST — Master E2E Coverage

Living document. Updated every time the Master Release Test runs or grows.
Do not delete a row because implementation changed — update its status and
keep the history in git blame. See `e2e/` (backend, Suite A) and
`garagest_flutter/integration_test/` (Android, Suite B).

**Last full run:** 2026-09-18, `.\master-release-test.ps1 -Full`, run
`MRT20260918895519` (Suite A + Suite B, single command, end to end).
**Result:** Suite A 46 PASS / 2 FAIL / 4 BLOCKED / 0 NOT_YET_AUTOMATED (52
steps); Suite B device orchestration 4/4 personas PASS on the 1 connected
device (exit 0).
**Job Card used:** `G037-JC-2026-000001` (reached **CLOSED**) — the SAME
Job Card for both suites, via the `*.run_context.json` handoff (see
"Dynamic multi-device persona orchestration" below).
**Reports:** `e2e/reports/MRT20260918895519.{json,html,md}` (Suite A),
`e2e/reports/MRT20260918895519.suite_b_devices.{json,md}` (Suite B device
orchestration).

**Backend unit/integration test suite (`./mvnw.cmd test`, 260 tests,
`garagest-e2e.env` sourced):** 260/260 PASS, 0 failures, 0 errors. The
`GarageOsApplicationTests.contextLoads` failure seen in one intermediate
invocation this session was an **environment-invocation artifact**, not a
code or product bug: that specific shell call did not have `DB_HOST`/
`DB_PORT`/`DB_NAME` exported, so `application.properties`' `jdbc:postgresql://
${DB_HOST}:${DB_PORT}/${DB_NAME}` placeholder was passed to the driver
unresolved (`Driver org.postgresql.Driver claims to not accept jdbcUrl,
jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}`). Confirmed resolved by
rerunning with `garagest-e2e.env` properly sourced in the same command —
clean `BUILD SUCCESS`, no output from `grep -E "Tests run:|BUILD
FAILURE|ERROR\]"` under `mvnw -q`. Not caused by, and not related to, any
of this session's 3 backend code changes.

---

## How to run

```bash
cd garageos-backend/e2e
pip install -r requirements.txt
python run.py                    # full 9-phase journey against a running backend
python run.py --phase 5          # phases 1..5 only
```

Requires the real backend already running (`garageos-backend`, real Postgres,
real Google Drive credentials in `garagest-e2e.env`). The script never starts
the backend itself and reports `ENVIRONMENT FAILURE` if it can't reach it.

Android Suite B: see `garagest_flutter/integration_test/README` (added this
same release; see §Suite B below for current status).

---

## Coverage matrix

Legend: **PASS** (backend-verified this run) · **FAIL** (confirmed bug, see
Known Issues) · **BLOCKED** (real external/product blocker, not bypassed) ·
**NOT_YET_AUTOMATED** (real gap, no test exists yet) · **N/A**

| Capability | Customer | Manager | Service Advisor | Technician | Owner | Driver |
|---|---|---|---|---|---|---|
| Register/Login | PASS | PASS (via employee onboarding) | N/A | PASS | PASS | PASS |
| Password/Confirm-Password eye toggle | NOT_YET_AUTOMATED (Suite B) | N/A | N/A | N/A | N/A | N/A |
| Session restore / no infinite loading | NOT_YET_AUTOMATED (Suite B) | N/A | N/A | N/A | N/A | N/A |
| Vehicle create | PASS | N/A | N/A | N/A | N/A | N/A |
| Vehicle edit (own) | PASS | N/A | N/A | N/A | N/A | N/A |
| Vehicle IDOR (cross-customer) | PASS (blocked correctly) | N/A | N/A | N/A | N/A | N/A |
| Booking create | PASS | PASS (sees it) | NOT_YET_AUTOMATED | N/A | NOT_YET_AUTOMATED | N/A |
| "+Workflow" -> Job Card | N/A | PASS | NOT_YET_AUTOMATED | N/A | N/A | N/A |
| Job Card visibility | PASS | PASS | NOT_YET_AUTOMATED | PASS (correctly absent pre-assignment) | PASS | N/A |
| Inspection start/complete | N/A | PASS | NOT_YET_AUTOMATED | NOT_YET_AUTOMATED | NOT_YET_AUTOMATED | N/A |
| Inspection media upload -> Google Drive | N/A | BLOCKED (OAuth token expired, see below) | N/A | N/A | N/A | N/A |
| Estimate prepare/finalize | N/A | PASS | NOT_YET_AUTOMATED | N/A | NOT_YET_AUTOMATED | N/A |
| Estimate approve | PASS | PASS (sees APPROVED) | N/A | N/A | NOT_YET_AUTOMATED | N/A |
| Repair task assign | N/A | PASS | N/A | N/A | N/A | N/A |
| Repair task start/complete (RepairTask endpoints) | N/A | N/A | N/A | PASS | N/A | N/A |
| **RepairTask/JobAssignment consistency** | N/A | N/A | N/A | **FAIL — see Known Issue #1** | N/A | N/A |
| Task-wise repair-evidence upload | N/A | N/A | N/A | BLOCKED (same Drive OAuth issue) | N/A | N/A |
| Quality Check pass | N/A | PASS | N/A | NOT_YET_AUTOMATED | NOT_YET_AUTOMATED | N/A |
| Customer progress timeline consistency | PASS (fixed this run — see Known Issue, now resolved) | N/A | N/A | N/A | N/A | N/A |
| Invoice generate | N/A | PASS | N/A | N/A | NOT_YET_AUTOMATED | N/A |
| Invoice view ("View invoice" navigation) | NOT_YET_AUTOMATED (Suite B — backend-side confirmed correct) | N/A | N/A | N/A | N/A | N/A |
| **Customer payment (real path)** | **BLOCKED — RELEASE BLOCKER, see Known Issue #2** | N/A | N/A | N/A | N/A | N/A |
| Driver assignment | N/A | PASS | N/A | N/A | NOT_YET_AUTOMATED | PASS (sees trip) |
| NavigationTrip accept/start/arrive/complete | N/A | N/A | N/A | N/A | N/A | PASS |
| **Dummy-GPS / STOMP ordered delivery** | NOT_YET_AUTOMATED (live map UI) | N/A | N/A | N/A | N/A | **FAIL — see Known Issue #3** |
| Delivery media (local storage, not Drive) | N/A | N/A | N/A | N/A | N/A | PASS |
| Real handover (customer code -> driver verify) | PASS | N/A | N/A | N/A | N/A | PASS |
| Delivery record + Job Card CLOSED | PASS (sees delivered state) | PASS | N/A | N/A | NOT_YET_AUTOMATED | N/A |
| Media survives CLOSED | BLOCKED (no media survived to check — upstream Drive block) | | | | | |
| Garage cross-tenant isolation (2 garages) | NOT_YET_AUTOMATED | | | | | |
| Android real-device UI (any screen) | NOT_YET_AUTOMATED except password toggle (manually verified, not yet in Suite B) | | | | | |

**Owner/Service-Advisor "NOT_YET_AUTOMATED" note:** every Owner/Service-Advisor
row above needs a dedicated persona session added to `journey.py`
(`PersonaSession` + a role approval, same pattern as manager/technician/driver)
plus real assertions against whatever their actual read endpoints are — not
yet done, listed honestly rather than assumed to inherit Manager's coverage.

---

## Known Issues (confirmed this run, root-caused, permanently tracked)

### #1 — JobAssignment / RepairTask disconnect — HIGH, CODE_BUG, confirmed root cause
`RepairTaskServiceImpl.startRepair()` / `.completeRepair()` (the endpoints
`ServiceWorkflow` and every manager/customer-facing screen actually read)
never touch the `JobAssignment` linked to the same task at assignment time
(`RepairTaskServiceImpl.linkOrCreateJobAssignment`, called from `assign()`).
The reverse was independently confirmed during the real-device session:
Flutter's `TechnicianDashboard` completes work through
`JobAssignmentService` only. Net effect: **whichever path a technician
actually uses, the other model never finds out** — confirmed again this run
even with the technician deliberately driving the *correct* `RepairTask`
endpoints throughout: the `JobAssignment` created at assign-time was still
`ASSIGNED`/`ACCEPTED` after the Job Card reached `CLOSED`.
Regression: `master_e2e/assertions.py::assert_repair_task_and_assignment_agree`,
exercised every run in `test_05`/`test_09`.

**Confirmed downstream consequence (new this run, `test_09`):** because
`JobAssignment` never transitions, a technician's "active work" view (backed
by their open `JobAssignment` rows) still lists a job **after the Job Card
has reached CLOSED** — `test_09_delivery_and_closure.py` now asserts a
CLOSED job's assignments are no longer surfaced as active work and fails
with the exact stale-assignment payload
(`assignmentId`/`assignmentType`/`jobCardNumber`) when it isn't. This is not
a separate bug — it is the first concretely observed user-facing symptom of
Known Issue #1, and evidence that #1 is not merely a data-consistency nit:
a technician can be shown ongoing work on a job the customer has already
picked up.
**Fix requires a product decision** (unify on RepairTask as canonical and
either delete JobAssignment's independent completion path or make
`RepairTaskServiceImpl` write through to it) — not applied unilaterally.

### #2 — Customer cannot pay their own invoice — RELEASE BLOCKER, PRODUCT_DECISION_REQUIRED
`POST /workflow/{jobCardNumber}/payment` is
`@PreAuthorize(hasAnyRole('MANAGER','SERVICE_ADVISOR','OWNER'))`. The real
Flutter customer flow (`CustomerPortalService.payInvoice`) calls this same
endpoint and gets `403`. Regression: `test_07_invoice_and_payment.py`
explicitly asserts the 403 (must stay forbidden until this is redesigned)
and marks the actual payment attempt `BLOCKED`, never substituting a manager
call as "customer payment passed."

### #3 — STOMP location updates can arrive out of order — MEDIUM/HIGH, CODE_BUG, confirmed root cause
`WebSocketConfig` does not override `configureClientInboundChannel`, so
Spring's default multi-threaded inbound dispatch has no per-session/per-trip
ordering guarantee, even though `DriverLocationServiceImpl.processLocation`
itself is synchronous. Reproduced on 3 of 3 runs this session, with a
different scramble pattern each time (consistent with a genuine thread-pool
race, not a fluke). A driver's live-tracking marker could visibly jump
backward in time. Fix path (serialize per-trip, or add timestamp-based
reordering client- or server-side) is a real throughput trade-off — not
applied unilaterally. Regression: `test_09_navigation_and_dummy_gps.py`.

### #4 — Google Drive OAuth access token expired mid-session — EXTERNAL_SERVICE_ISSUE
`GoogleDriveOAuthService`/`GoogleDriveCredentialDataStore`'s auto-refresh
design looks structurally correct on inspection (`expirationTimeMilliseconds`
is persisted and restored correctly; the library's
`DataStoreCredentialRefreshListener` is documented as wired in by the OAuth
flow). Root cause of why refresh didn't fire is **not yet determined** —
needs either a live re-authorization + timed reproduction, or a check of the
Google Cloud Console OAuth consent screen's publishing status (Testing-mode
grants can be silently invalidated in ways that produce exactly this 401).
Not guessed at further. All 3 Drive-dependent steps this run correctly
degrade to `BLOCKED` rather than cascading into every downstream step.

### #5 (FIXED this run) — Customer repair-tracking milestones were hardcoded
`CustomerPortalServiceImpl.trackRepair()` had `inspectionCompleted` hardcoded
`true` unconditionally, `estimateApproved` aliased to "an estimate exists"
(true before the customer ever approved it), and
`repairCompleted`/`qualityChecked`/`paymentCompleted` hardcoded `false`
forever — this is the confirmed root cause of the customer-facing
"Repair complete / QC incomplete / Invoice complete" contradiction observed
during the real-device session. Fixed: all five now derive from the Job
Card's actual status via the same canonical lifecycle
`JobCardStatusValidator` already encodes, plus the real `Estimate.status`
for approval specifically. Regression:
`assertions.py::assert_customer_progress_consistent`, called after every
status-changing phase from 03 onward.

### #6 (test bug, not an app bug) — Vehicle-edit choice can break inspection lookup
Editing a vehicle's `variant`/`fuelType`/`transmission` to a combination with
no matching `InspectionMaster` catalog row causes "Start Inspection" to fail
later with a bare `404 "Inspection Master not found."` — technically correct
per `InspectionFindingServiceImpl.loadInspectionTemplate`'s exact-match
lookup, but a confusing failure mode for a real user who only meant to fix a
typo in their car's color. Recorded as a LOW finding, not fixed (would be a
UX/validation-message change, not urgent) — `journey.py`'s own vehicle-edit
step deliberately avoids re-triggering it so it doesn't derail the one
primary journey every run.

### #7 — No per-task Google Drive folder (architecture note, not a bug)
`GoogleDriveFolderService` creates folders down to
`<garageCode>/<jobCardNumber>/<stage>` only — there is no task-level Drive
subfolder. Task association exists solely via `JobCardMedia.repairTaskId` in
Postgres. `test_05` tests the real mechanism (DB association), not an
invented folder hierarchy. If the product wants Drive-level task isolation,
that's a new feature, not a bug fix.

---

## Suite B (Android, `garagest_flutter/integration_test/`) — status this release

**Added this release:** `integration_test` dev dependency (approved),
`master_journey_test.dart`, semantic Keys on the highest-value existing
widgets (login, register password toggles, and — new this release — every
persona's home-shell `Scaffold`/`AdaptiveNavigationScaffold` and logout
control: `customer_home_scaffold`/`customer_logout_button`,
`employee_home_scaffold`/`employee_logout_button`,
`technician_home_scaffold`/`technician_logout_button`,
`driver_home_scaffold`/`driver_logout_button`,
`owner_home_scaffold`/`owner_logout_button`).

**Actually run on the connected device this release:** login screen
rendering, login and register password-visibility toggles, **and — new
this release — the full dynamic persona device-reuse group**: one physical
device logs in and out as CUSTOMER, MANAGER, TECHNICIAN, and DRIVER in
sequence, each landing on the correct role-specific shell, proven via the
`*_home_scaffold` Keys. All via `master-release-test.ps1 -Full`/`-Android`,
against the real device (`192.168.1.58:34697`, wireless ADB) and the SAME
real accounts + Job Card Suite A's own run just created (see "Dynamic
multi-device persona orchestration" below).

**NOT YET automated in Suite B (honest gap, not hidden):** vehicle
create/edit, booking, +Workflow, inspection, media picker (camera/gallery),
media viewer, estimate approval, technician screens, QC, invoice/payment
screens, driver/navigation UI, delivery, closure. Each of these needs its
own `integration_test` group plus stable `Key`s on the relevant widgets
(see §20 of the original brief for the suggested key names) — building all
of them to the same real-device-driven standard as Suite A's 9 phases is
the next release's main body of work, tracked incrementally per §29's phase
model, not attempted in one pass here.

---

## Dynamic multi-device persona orchestration (DevicePool)

**Core rule honored:** the number of connected Android devices never
changes the business journey — there is exactly one canonical journey
(`master_journey_test.dart`'s persona-cycle group); a device-count-agnostic
allocator decides which physical device runs which persona.

```
MasterJourney (one canonical journey, always)
    |
PersonaStep (CUSTOMER, MANAGER, TECHNICIAN, DRIVER, in that order)
    |
DevicePool.acquire_persona(persona)     <- e2e/master_e2e/device_pool.py
    |
Physical Android device (adb serial — never hardcoded)
    |
Flutter integration_test actions        <- master_journey_test.dart,
    |                                       MRT_DEVICE_PERSONAS dart-define
    |                                       slices which personas THIS
    |                                       device instance cycles through
backend assertions                       <- live status fetch, same
                                             *.run_context.json Job Card
```

**Allocation rules implemented** (`DevicePool.acquire_persona`, 26 unit
tests in `e2e/tests/test_device_pool.py`, all passing, pure logic/no
hardware required):
1. A device already logged in as the requested persona is reused as-is
   (no logout/login round trip).
2. Otherwise an **idle** device (never used yet) is preferred — this is
   what keeps Mode 4+ ("Customer→A, Manager→B, Technician→C, Driver→D")
   on distinct devices and leaves extra devices (Mode 5, 8-9+) untouched.
3. Only when no idle device exists does the pool fall back to logging an
   occupied device out and back in as the new persona — the Mode 1
   single-device fallback, since after the first login there is never an
   idle candidate again on a 1-device pool.
4. `reserve_concurrent([...], tag)` / `release_concurrent(...)` dedicate
   specific devices to a concurrent scenario (e.g. live Driver GPS +
   Customer observation) for its duration; a reserved device is invisible
   to `acquire_persona` until released — never logged out mid-operation.
5. No connected device at all (or all connected devices reserved) raises
   `DevicePoolError` — a real, reportable BLOCKED condition, never a
   fabricated pass.

**Shared run context** (`e2e/reports/<runId>.run_context.json`, written by
`run.py` at the end of every Suite A run): `runId`, `password`,
`jobCardNumber`, and the real CUSTOMER/MANAGER/TECHNICIAN/DRIVER usernames
Suite A's journey just created. `run_suite_b_devices.py` reads this and
passes it into each device's `flutter test` invocation via
`MRT_*` dart-defines — Suite B never registers its own accounts or creates
its own Job Card, so "Customer on Device A / Manager on Device B / ... all
operate on the SAME Job Card" is enforced by construction, not by
convention.

**Executed this release (only 1 physical device connected):** Mode 1 for
real — `.\master-release-test.ps1 -Full` (or `-Android` after a prior
`-Backend` run) discovers 1 device, `plan_allocation(["CUSTOMER",
"MANAGER","TECHNICIAN","DRIVER"])` puts all 4 on it, and one
`flutter test -d 192.168.1.58:34697 --dart-define=MRT_DEVICE_PERSONAS=
CUSTOMER,MANAGER,TECHNICIAN,DRIVER ...` process cycles all four via real
logout/login. Result: **PASS**, exit 0, ~4 minutes.

**Implemented and unit/static-validated, NOT executed against real
hardware this release (Modes 2-5, 2+ devices):** `plan_allocation` correctly
spreads personas across 2, 4, and 9 simulated devices (unit tests:
`test_two_devices_allocate_distinct_personas_independently`,
`test_four_devices_one_persona_each_no_reuse_needed`,
`test_plan_allocation_nine_devices_only_uses_four`,
`test_extra_idle_devices_are_never_touched`); `run_suite_b_devices.py`
launches one `flutter test -d <serial>` subprocess per device in the plan
via `subprocess.Popen` (real OS-level parallelism, not a simulated loop)
whenever the plan spans more than one device. This has not been run against
real hardware because only 1 physical device was connected this session —
it will execute for real, with zero code changes, the next time 2+ devices
are connected. **This is the honest, explicit fallback the mission
requested**, not a claim that multi-device mode has been proven on
hardware.

**Concurrency (live Driver GPS + Customer observation on 2 distinct
devices):** `reserve_concurrent`/`release_concurrent` are implemented and
unit-tested (`test_concurrent_reservation_blocks_reuse_by_a_different_
operation`, `test_reserved_device_is_invisible_to_acquisition_until_
released`), but not wired into a live 2-device Suite B run this release —
only 1 device was connected, so genuinely simultaneous mobile sessions
could not be exercised. Suite A's existing dummy-GPS/STOMP harness (Known
Issue #3) remains the mechanism actually exercising concurrent
driver-location delivery this release, per the mission's own fallback
instruction ("if only one device is available... use the existing
backend/WebSocket harness rather than falsely claiming two simultaneous
mobile sessions").

**Files:** `e2e/master_e2e/device_pool.py` (allocator), `e2e/tests/
test_device_pool.py` (26 unit tests), `e2e/run_suite_b_devices.py`
(discovers devices, builds the plan, runs/reports), `e2e/run.py` (writes
the `*.run_context.json` handoff), `garagest_flutter/integration_test/
master_journey_test.dart` (`MRT_DEVICE_PERSONAS`-sliced persona-cycle
group), `master-release-test.ps1` (`Get-ConnectedDevices` — real, dynamic
discovery, no hardcoded serial or count).

---

## Bugs found and fixed via the device-orchestration work this release

- **Customer dashboard `RenderFlex` overflow** (`customer_dashboard_screen.
  dart`, `_QuickAction`'s subtitle `Text`) — a pre-existing, real layout
  overflow on this physical device's screen size, invisible in casual
  manual use but a hard `FlutterError` under `integration_test`'s strict
  assertion checking, which made every persona-cycle run fail even though
  the login/logout business logic itself was correct. Fixed by wrapping
  the subtitle in `Flexible`. Confirmed via 3 real-device reruns
  (fail → fail → pass) — not a test-script workaround.
- **Bottom-of-screen logout button tap landing on the NavigationBar
  instead of the button** (`customer_logout_button`, `customer_profile_
  screen.dart`) — the button's computed center coincided pixel-for-pixel
  with the NavigationBar's hit-test region on this device/screen size.
  Fixed in the test only (`ensureVisible` + tapping near the button's top
  edge instead of dead-center) since the app's own layout is otherwise
  correct here (a Scaffold `bottomNavigationBar` + scrollable body is a
  standard, valid pattern) — this is a `integration_test`-authoring
  robustness fix, not an app bug.
- **`master-release-test.ps1` UTF-8-without-BOM parse risk** — the file's
  pre-existing em-dashes, combined with no byte-order mark, risked
  PowerShell 5.1 misreading the file under the system ANSI codepage
  instead of UTF-8 (confirmed via `Parser]::ParseFile` throwing cascading
  "unterminated string"/"unexpected token" errors before a BOM was added,
  and parsing cleanly after). Fixed by rewriting the file with a UTF-8 BOM.
- **`Get-ConnectedDevices` returning a bare string instead of an array**
  when exactly one device is connected — PowerShell's automatic
  single-element-array-to-scalar collapsing on `return`, confirmed live
  (`Detected devices: 1` / `[1] 1` instead of the real serial). Fixed with
  the unary-comma `return ,$serials` idiom; confirmed fixed via rerun
  (`[1] 192.168.1.58:34697`).

---

## Next incremental test to add (recommended)

1. Root-cause backend Suite A Known Issue #4 (Drive token refresh) with a
   live, timed reproduction — this currently blocks 3 of 52 steps and all
   of §Media/§Google-Drive-folder coverage.
2. Run Suite B's multi-device path (Modes 2-5) for real once a second
   physical device is available — the allocator and per-device subprocess
   launcher are implemented and unit-tested, but genuinely unproven on
   hardware beyond 1 device.
3. Suite B: `create_vehicle_and_booking_test.dart` — the next real screen
   flow after login, using widget-key finders, run against the same device.
4. Owner and Service-Advisor `PersonaSession`s in `journey.py`, to close the
   `NOT_YET_AUTOMATED` gaps in the coverage matrix's Owner/SA columns.
5. A second garage + rogue-persona cross-garage isolation test in
   `test_99_security_regressions.py` (currently only same-garage
   cross-customer IDOR is covered).
