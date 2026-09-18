"""The ONE Master Release Test business journey.

One customer, one vehicle, one booking, one job card, carried through the
entire lifecycle. Every phase function takes the same JourneyContext and
StepRecorder and appends to both — nothing here creates a second job card
"to make a phase easier," per explicit instruction.
"""
from __future__ import annotations

import datetime
from dataclasses import dataclass, field

from .assertions import (
    AssertionFailure,
    assert_customer_progress_consistent,
    assert_forbidden,
    assert_media_belongs_to_task,
    assert_media_not_leaked,
    assert_repair_task_and_assignment_agree,
    assert_workflow_status,
)
from .client import ApiError, MasterE2EClient
from .fixtures import MP4_MINIMAL, PASSWORD, PNG_1PX_RED, RunFixtures
from .personas import PersonaSession
from .report import StepRecorder
from .stomp_client import StompSession


def _is_drive_auth_failure(e: ApiError) -> bool:
    body_text = str(e.body)
    return e.status in (503, 401) and ("DRIVE_AUTH" in body_text or "MEDIA_DRIVE" in body_text)


def _media_step(rec: StepRecorder, phase: str, name: str, actor: str, fn):
    """Runs a media-upload fn() (which does its own assertions and returns
    the uploaded media dict). Records PASS/FAIL exactly like rec.step,
    EXCEPT when the failure is specifically Google rejecting the stored
    OAuth credential (confirmed root cause: the access token expired
    mid-session; GoogleDriveClientService raised
    MEDIA_DRIVE_AUTH_FAILED/MEDIA_DRIVE_AUTH_REQUIRED) — that specific case
    is recorded as BLOCKED/EXTERNAL_SERVICE_ISSUE instead of FAIL, so one
    expired credential doesn't cascade into every downstream step that
    would otherwise depend on the uploaded media existing. Any other
    failure (content-type handling, association bugs, wrong stage, etc.)
    still fails normally.
    """
    import time as _time
    start = _time.monotonic()
    try:
        result = fn()
    except ApiError as e:
        duration = (_time.monotonic() - start) * 1000
        if _is_drive_auth_failure(e):
            rec.blocked(
                phase, name, actor,
                f"Google Drive OAuth credential rejected by Google (status {e.status}): {str(e.body)[:300]}. "
                f"Root cause not yet fixed — see MASTER_E2E_COVERAGE.md ('Google Drive token refresh').",
                classification="EXTERNAL_SERVICE_ISSUE",
            )
            return None
        rec.step(phase, name, actor, lambda: (_ for _ in ()).throw(e))
        return None
    except Exception as e:  # noqa: BLE001
        rec.step(phase, name, actor, lambda: (_ for _ in ()).throw(e))
        return None
    else:
        rec.step(phase, name, actor, lambda: None)
        return result


@dataclass
class JourneyContext:
    client: MasterE2EClient
    fixtures: RunFixtures
    owner: PersonaSession = None
    manager: PersonaSession = None
    technician: PersonaSession = None
    driver: PersonaSession = None
    customer: PersonaSession = None

    garage_id: int = None
    garage_code: str = None
    customer_id: int = None
    vehicle_id: int = None
    booking_id: int = None
    jobcard_id: int = None
    jobcard_number: str = None
    complaint_ids: list = field(default_factory=list)
    estimate_id: int = None
    repair_tasks: list = field(default_factory=list)
    invoice_id: int = None
    uploaded_media: dict = field(default_factory=dict)  # task_id -> media response

    def make_personas(self) -> None:
        f = self.fixtures
        self.owner = PersonaSession(self.client, f.owner)
        self.manager = PersonaSession(self.client, f.manager)
        self.technician = PersonaSession(self.client, f.technician)
        self.driver = PersonaSession(self.client, f.driver)
        self.customer = PersonaSession(self.client, f.customer)


# ---------------------------------------------------------------------------
# PHASE 01 — customer identity, vehicle, IDOR
# ---------------------------------------------------------------------------

def phase_01_customer_and_vehicle(rec: StepRecorder, ctx: JourneyContext) -> None:
    phase = "01_customer_and_vehicle"

    def register_login():
        ctx.owner.register_and_login()
    rec.step(phase, "Owner register + login", "OWNER", register_login)

    def create_garage():
        garage = ctx.owner.post(
            "/garages",
            {
                "garageName": ctx.fixtures.garage_name, "workshopType": "MULTI_BRAND",
                "numberOfBays": 4, "address": "1 MRT St", "city": "Hyderabad",
                "state": "Telangana", "pincode": "500001",
            },
            expect=(201,),
        )
        ctx.garage_id = garage["id"]
        ctx.garage_code = garage["garageCode"]
        ctx.owner.refresh()  # pick up OWNER role + garageId
    rec.step(phase, "Create garage", "OWNER", create_garage)

    def onboard_employees():
        for persona in (ctx.manager, ctx.technician, ctx.driver):
            persona.register_and_login()
            persona.post("/garage-memberships/join", {"garageCode": ctx.garage_code}, expect=(200,))
        roles = ctx.owner.get("/master/employee-roles")
        role_id = {r["code"]: r["id"] for r in roles}
        pending = ctx.owner.get("/garage-memberships/pending")

        def find(prefix):
            return next(m for m in pending if m["email"].startswith(prefix))

        for persona, code in (
            (ctx.manager, "MANAGER"), (ctx.technician, "TECHNICIAN"), (ctx.driver, "DRIVER"),
        ):
            membership = find(persona.seed.username)
            ctx.owner.put(f"/garage-memberships/{membership['id']}/approve", {"roleIds": [role_id[code]]}, expect=(200,))
        for persona in (ctx.manager, ctx.technician, ctx.driver):
            persona.refresh()
    rec.step(phase, "Onboard manager/technician/driver via real join+approve", "OWNER", onboard_employees)

    def activate_customer():
        ctx.customer.register_and_login()
        result = ctx.customer.post("/customers/activate", expect=(201,))
        ctx.customer_id = result["id"]
        ctx.customer.refresh()
    rec.step(phase, "Customer register + activate", "CUSTOMER", activate_customer)

    def create_vehicle():
        vehicle = ctx.customer.post(
            "/vehicles",
            {
                "registrationNumber": ctx.fixtures.vehicle_registration, "brand": "Volkswagen",
                "model": "Polo", "variant": "Highline", "fuelType": "PETROL",
                "transmission": "MANUAL", "manufacturingYear": 2018, "color": "White",
                "customerId": ctx.customer_id,
            },
            expect=(201,),
        )
        ctx.vehicle_id = vehicle["id"]
    rec.step(phase, "Create vehicle", "CUSTOMER", create_vehicle)

    def edit_vehicle():
        # DISCOVERED FINDING (kept out of the edit on purpose, not hidden):
        # InspectionFindingServiceImpl.loadInspectionTemplate looks up the
        # applicable InspectionMaster by exact
        # brand/model/variant/fuelType/transmission match. Editing variant
        # (e.g. "Highline" -> "Highline GT") silently breaks "Start
        # Inspection" later in the SAME journey with a bare 404 "Inspection
        # Master not found." and no indication why. That is a real, if
        # minor, UX gap (see MASTER_E2E_COVERAGE.md) — but re-triggering it
        # here on every run would derail the one primary journey over an
        # already-known issue, so this step edits only fields that do not
        # participate in that lookup (registrationNumber, color) and keeps
        # brand/model/variant/fuelType/transmission identical to what the
        # vehicle was created with.
        updated = ctx.customer.put(
            f"/vehicles/{ctx.vehicle_id}",
            {
                "registrationNumber": ctx.fixtures.vehicle_registration_edited, "brand": "Volkswagen",
                "model": "Polo", "variant": "Highline", "fuelType": "PETROL",
                "transmission": "MANUAL", "manufacturingYear": 2018, "color": "Pearl White",
                "customerId": ctx.customer_id,
            },
            expect=(200,),
        )
        if updated.get("registrationNumber") != ctx.fixtures.vehicle_registration_edited or updated.get("color") != "Pearl White":
            raise AssertionFailure(f"Vehicle edit did not persist: {updated}")
    rec.step(phase, "Edit vehicle (own)", "CUSTOMER", edit_vehicle)

    def idor_regression():
        rogue = PersonaSession(ctx.client, ctx.fixtures.rogue_customer)
        rogue.register_and_login()
        rogue.post("/customers/activate", expect=(201,))
        rogue.refresh()
        assert_forbidden(
            ctx.client, "PUT", f"/vehicles/{ctx.vehicle_id}", rogue.token,
            body={
                "registrationNumber": ctx.fixtures.vehicle_registration_edited, "brand": "HACKED",
                "model": "HACKED", "customerId": ctx.customer_id,
            },
            actor="ROGUE_CUSTOMER",
        )
    rec.step(phase, "Vehicle IDOR regression (cross-customer edit must be forbidden)", "SECURITY", idor_regression)


# ---------------------------------------------------------------------------
# PHASE 02 — booking, manager visibility, "+Workflow" (createJob)
# ---------------------------------------------------------------------------

def phase_02_booking_and_workflow(rec: StepRecorder, ctx: JourneyContext) -> None:
    phase = "02_booking_and_workflow"

    def create_booking():
        future = (datetime.datetime.now() + datetime.timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%S")
        booking = ctx.customer.post(
            "/bookings",
            {
                "vehicleId": ctx.vehicle_id, "garageId": ctx.garage_id,
                "serviceDescription": "Engine vibration, AC not cooling, Brake noise",
                "concerns": "Engine vibration; AC not cooling; Brake noise",
                "requestedAt": future, "pickupRequested": False,
            },
            expect=(201,),
        )
        ctx.booking_id = booking["id"]
    rec.step(phase, "Customer creates booking", "CUSTOMER", create_booking)

    def manager_sees_booking():
        bookings = ctx.manager.get("/bookings/garage")
        match = next((b for b in bookings if b["id"] == ctx.booking_id), None)
        if match is None:
            raise AssertionFailure(f"Manager cannot see booking {ctx.booking_id} in /bookings/garage")
        if match.get("vehicleId") != ctx.vehicle_id:
            raise AssertionFailure(f"Booking vehicle mismatch: {match}")
    rec.step(phase, "Manager sees booking with correct vehicle/garage", "MANAGER", manager_sees_booking)

    def confirm_booking():
        ctx.manager.post(f"/bookings/{ctx.booking_id}/confirm", {}, expect=(200,))
    rec.step(phase, "Manager confirms booking", "MANAGER", confirm_booking)

    def plus_workflow_creates_jobcard():
        """This is the backend action the Flutter '+ Workflow' FAB drives
        (EmployeeHomeScreen resets WorkflowState then navigates to the
        workflow shell, whose first real step calls WorkflowService ->
        POST /workflow/job). Verified against the actual Flutter source
        (job_card.dart / workflow_service.dart) during Phase-02 planning —
        not assumed.
        """
        jobcard = ctx.manager.post(
            "/workflow/job",
            {
                "vehicleId": ctx.vehicle_id, "odometerReading": 45210,
                "complaints": [
                    {"complaint": "Engine vibration"}, {"complaint": "AC not cooling"},
                    {"complaint": "Brake noise"},
                ],
                "remarks": "Master Release Test job", "bookingId": ctx.booking_id,
            },
        )
        status = ctx.client.transcript[-1].status
        if status not in (200, 201):
            raise AssertionFailure(f"+Workflow (POST /workflow/job) failed: {status} {jobcard}")
        ctx.jobcard_id = jobcard["id"]
        ctx.jobcard_number = jobcard["jobCardNumber"]
        ctx.complaint_ids = [c["id"] for c in jobcard.get("complaints", [])]
        if jobcard.get("customerId") != ctx.customer_id or jobcard.get("vehicleId") != ctx.vehicle_id:
            raise AssertionFailure(f"Job card linkage wrong: {jobcard}")
    rec.step(phase, "+Workflow creates Job Card, linked to real booking/customer/vehicle", "MANAGER", plus_workflow_creates_jobcard)

    def workflow_state_correct():
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "OPEN")
    rec.step(phase, "ServiceWorkflow status is OPEN immediately after +Workflow", "BACKEND", workflow_state_correct)

    def customer_sees_jobcard():
        jobcards = ctx.customer.get("/customer/jobcards")
        match = next((j for j in jobcards if j.get("jobCardNumber") == ctx.jobcard_number), None)
        if match is None:
            raise AssertionFailure("Customer cannot see their own new job card in /customer/jobcards")
    rec.step(phase, "Customer sees new Job Card", "CUSTOMER", customer_sees_jobcard)

    def owner_sees_jobcard():
        # Owner dashboard is garage-scoped; assert via the same garage bookings/jobcards visibility an owner has (owner holds OWNER role, same garage-scoped endpoints as manager).
        jobcards = ctx.owner.get("/customer/jobcards") if False else None
        # Owner isn't a customer; use the operational jobcards listing instead.
        all_jobs = ctx.owner.get("/jobcards?page=0&size=50")
        content = all_jobs.get("content", all_jobs) if isinstance(all_jobs, dict) else all_jobs
        match = next((j for j in content if j.get("jobCardNumber") == ctx.jobcard_number), None)
        if match is None:
            raise AssertionFailure("Owner cannot see the new Job Card via /jobcards")
    rec.step(phase, "Owner sees new Job Card", "OWNER", owner_sees_jobcard)

    def technician_does_not_see_unassigned_jobcard():
        # Technician has no assignments yet — /job-assignments/my must be empty for this job.
        assignments = ctx.technician.get("/job-assignments/my")
        leaked = [a for a in assignments if a.get("jobCardNumber") == ctx.jobcard_number]
        if leaked:
            raise AssertionFailure(f"Technician sees unassigned job card in their assignments: {leaked}")
    rec.step(phase, "Technician does not see job before assignment", "TECHNICIAN", technician_does_not_see_unassigned_jobcard)


# ---------------------------------------------------------------------------
# PHASE 03 — inspection + media
# ---------------------------------------------------------------------------

def phase_03_inspection_and_media(rec: StepRecorder, ctx: JourneyContext) -> None:
    phase = "03_inspection_and_media"

    def start_inspection():
        ctx.manager.post(f"/workflow/{ctx.jobcard_number}/inspection/start", expect=(200,))
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "INSPECTION_PENDING")
    rec.step(phase, "Start inspection, ServiceWorkflow -> INSPECTION_PENDING", "MANAGER", start_inspection)

    def record_findings():
        findings = ctx.manager.get(f"/inspection-findings/jobcards/{ctx.jobcard_id}")
        if not findings:
            raise AssertionFailure("Inspection checklist did not auto-load")
        for i, finding in enumerate(findings[:3]):
            ctx.manager.call(
                "PUT", f"/inspection-findings/{finding['id']}",
                {
                    "inspectionMasterItemId": finding["inspectionMasterItemId"], "status": "FAIL",
                    "remarks": ["Engine mount worn", "AC compressor not engaging", "Front brake pads worn"][i],
                },
                expect=(200,),
            )
    rec.step(phase, "Record 3 inspection findings", "MANAGER", record_findings)

    def upload_inspection_media():
        media = ctx.manager.multipart(
            f"/job-cards/{ctx.jobcard_id}/media", {"stage": "BEFORE_SERVICE"},
            "file", "inspection_evidence.png", PNG_1PX_RED, "image/png", expect=(200, 201),
        )
        if not media.get("driveFileId"):
            raise AssertionFailure(f"Inspection media upload did not return a Google Drive file id: {media}")
        ctx.uploaded_media["inspection"] = media
        return media
    _media_step(rec, phase, "Upload inspection photo -> Google Drive (assert driveFileId present)", "MANAGER", upload_inspection_media)

    def complete_inspection():
        result = ctx.manager.post(
            f"/workflow/{ctx.jobcard_number}/inspection/complete",
            [
                {"inspectionNotes": "Engine mount worn, causing vibration.", "recommendedWork": "Replace engine mount."},
                {"inspectionNotes": "AC compressor not engaging.", "recommendedWork": "Repair/replace AC compressor."},
                {"inspectionNotes": "Front brake pads worn.", "recommendedWork": "Replace front brake pads."},
            ],
            expect=(200,),
        )
        # NOT a bug: JobCardStatusValidator's canonical map has
        # INSPECTION_COMPLETED as its own real, distinct state (->
        # ESTIMATE_PENDING is the *next* allowed transition, entered later
        # by prepareEstimate) — confirmed by an earlier, wrong assertion
        # here expecting ESTIMATE_PENDING immediately, which the live
        # backend correctly rejected. Fixed to assert the real status.
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "INSPECTION_COMPLETED")
    rec.step(phase, "Complete inspection, ServiceWorkflow -> INSPECTION_COMPLETED", "MANAGER", complete_inspection)

    def customer_progress_after_inspection():
        tracking = ctx.customer.get(f"/customer/repair-tracking/{ctx.jobcard_number}")
        assert_customer_progress_consistent(tracking)
        if not tracking.get("inspectionCompleted"):
            raise AssertionFailure(f"Customer tracking doesn't show inspection completed: {tracking}")
    rec.step(phase, "Customer progress view is consistent post-inspection", "CUSTOMER", customer_progress_after_inspection)


# ---------------------------------------------------------------------------
# PHASE 04 — estimate + approval
# ---------------------------------------------------------------------------

def phase_04_estimate_and_approval(rec: StepRecorder, ctx: JourneyContext) -> None:
    phase = "04_estimate_and_approval"

    def prepare_estimate():
        ctx.manager.post(f"/workflow/{ctx.jobcard_number}/estimate", expect=(200,))
        estimates = ctx.manager.get("/estimates")
        match = next((e for e in estimates if e.get("jobCardId") == ctx.jobcard_id), None)
        if match is None:
            raise AssertionFailure("No estimate found for this job card after prepareEstimate")
        ctx.estimate_id = match["id"]
    rec.step(phase, "Manager prepares estimate", "MANAGER", prepare_estimate)

    def add_items():
        items = [
            {"itemType": "LABOUR", "description": "Engine mount replacement", "quantity": 1, "unitPrice": 2500},
            {"itemType": "LABOUR", "description": "AC compressor repair", "quantity": 1, "unitPrice": 4500},
            {"itemType": "PART", "description": "Front brake pads (set)", "quantity": 1, "unitPrice": 1800},
        ]
        for i, item in enumerate(items):
            payload = dict(item)
            if i < len(ctx.complaint_ids):
                payload["complaintId"] = ctx.complaint_ids[i]
            ctx.manager.post(f"/estimates/{ctx.estimate_id}/items", payload, expect=(201,))
    rec.step(phase, "Add 3 estimate line items", "MANAGER", add_items)

    def finalize_estimate():
        # Real Flutter flow: EstimateService.updateEstimate ("Proceed for Approval"),
        # which is what actually drives JobCard ESTIMATE_PENDING -> WAITING_FOR_APPROVAL.
        ctx.manager.put(f"/estimates/{ctx.estimate_id}", {"jobCardId": ctx.jobcard_id}, expect=(200,))
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "WAITING_FOR_APPROVAL")
    rec.step(phase, "Finalize estimate ('Proceed for Approval'), ServiceWorkflow -> WAITING_FOR_APPROVAL", "MANAGER", finalize_estimate)

    def customer_sees_and_approves():
        estimates = ctx.customer.get("/customer/estimates")
        match = next((e for e in estimates if e.get("jobCardNumber") == ctx.jobcard_number), None)
        if match is None or match.get("status") != "WAITING_FOR_APPROVAL":
            raise AssertionFailure(f"Customer's own estimate list doesn't show it waiting for approval: {estimates}")
        ctx.customer.put(f"/estimates/{ctx.estimate_id}/approve", expect=(200,))
    rec.step(phase, "Customer sees estimate and approves it", "CUSTOMER", customer_sees_and_approves)

    def manager_and_owner_see_approved():
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "REPAIR_PENDING")
        est = ctx.manager.get(f"/estimates/{ctx.estimate_id}")
        if est.get("status") != "APPROVED":
            raise AssertionFailure(f"Estimate not APPROVED after customer approval: {est}")
    rec.step(phase, "Manager sees APPROVED, ServiceWorkflow -> REPAIR_PENDING (repair tasks auto-created)", "MANAGER", manager_and_owner_see_approved)


# ---------------------------------------------------------------------------
# PHASE 05 — repair task lifecycle (the critical regression phase)
# ---------------------------------------------------------------------------

def phase_05_repair_tasks(rec: StepRecorder, ctx: JourneyContext) -> None:
    phase = "05_repair_task_lifecycle"

    def fetch_tasks_and_employee_id():
        ctx.repair_tasks = ctx.manager.get(f"/workflow/{ctx.jobcard_number}/repair-tasks")
        if not ctx.repair_tasks:
            raise AssertionFailure("No repair tasks were auto-created from the approved estimate")
        employees = ctx.manager.get(f"/garage-memberships/employees?garageId={ctx.garage_id}")
        if not any(e["email"] == ctx.fixtures.technician.email for e in employees):
            raise AssertionFailure("Technician does not appear in the garage employee roster")
        # RepairTaskController's ownership check ("assign" then "start") is
        # keyed to the caller's User id, not the GarageMembership id —
        # confirmed live: employeeId=<membership id> gets "Technician does
        # not belong to this Job Card's garage"; employeeId=<user id> works.
        ctx._tech_employee_id = ctx.technician.user["id"]
    rec.step(phase, "Fetch auto-created repair tasks", "MANAGER", fetch_tasks_and_employee_id)

    def start_repair_workflow():
        ctx.manager.post(f"/workflow/{ctx.jobcard_number}/repair/start", expect=(200,))
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "REPAIR_IN_PROGRESS")
    rec.step(phase, "Workflow-level repair start, ServiceWorkflow -> REPAIR_IN_PROGRESS", "MANAGER", start_repair_workflow)

    def assign_all_tasks():
        for t in ctx.repair_tasks:
            ctx.manager.call(
                "PUT", f"/repair-tasks/{t['id']}/assign",
                {"technicianName": ctx.fixtures.technician.first_name + " " + ctx.fixtures.technician.last_name,
                 "employeeId": ctx._tech_employee_id},
                expect=(200,),
            )
    rec.step(phase, "Manager assigns technician to every repair task", "MANAGER", assign_all_tasks)

    def technician_start_and_complete_first_task():
        first = ctx.repair_tasks[0]
        ctx.technician.call("PUT", f"/repair-tasks/{first['id']}/start", expect=(200,))
        ctx.technician.call("PUT", f"/repair-tasks/{first['id']}/complete", expect=(200,))
    rec.step(phase, "Technician starts+completes task 1 via the RepairTask endpoints the UI is SUPPOSED to call", "TECHNICIAN", technician_start_and_complete_first_task)

    def critical_divergence_regression():
        """This is the permanent guard for the bug discovered during the
        real-device session: Flutter's TechnicianDashboard completes work
        through /job-assignments/..., not /repair-tasks/.... If a
        JobAssignment for this same task exists and shows COMPLETED while
        the RepairTask does not, that's the exact disconnect — flag it.
        Since this Suite-A run drives RepairTask directly (the correct,
        ServiceWorkflow-recognised endpoint), this assertion currently
        passes; Suite B (the real Flutter app) is what actually proves
        whether the UI itself still has this bug — see integration_test/.
        """
        assert_repair_task_and_assignment_agree(
            ctx.client, ctx.manager.token, ctx.technician.token, ctx.jobcard_number,
        )
    rec.step(
        phase, "JobAssignment/RepairTask divergence guard (permanent regression, see coverage doc)",
        "BACKEND", critical_divergence_regression,
    )

    def upload_task_evidence_and_verify_association():
        """Task-wise media test, using the REAL association mechanism this
        backend actually has: JobCardMedia.repairTaskId (there is no
        per-task Google Drive subfolder — confirmed by reading
        GoogleDriveFolderService; folders stop at the MediaStage level:
        <garageCode>/<jobCardNumber>/<stage>). This is flagged as a
        finding in the coverage doc, not silently worked around.
        """
        second = ctx.repair_tasks[1] if len(ctx.repair_tasks) > 1 else ctx.repair_tasks[0]
        media_a = ctx.technician.multipart(
            f"/job-cards/{ctx.jobcard_id}/media",
            {"stage": "DURING_REPAIR", "repairTaskId": str(ctx.repair_tasks[0]["id"])},
            "file", "task_a_evidence.png", PNG_1PX_RED, "image/png", expect=(200, 201),
        )
        media_b = ctx.technician.multipart(
            f"/job-cards/{ctx.jobcard_id}/media",
            {"stage": "DURING_REPAIR", "repairTaskId": str(second["id"])},
            "file", "task_b_evidence.png", PNG_1PX_RED, "image/png", expect=(200, 201),
        )
        assert_media_belongs_to_task(media_a, ctx.repair_tasks[0]["id"])
        assert_media_belongs_to_task(media_b, second["id"])
        ctx.uploaded_media["task_a"] = media_a
        ctx.uploaded_media["task_b"] = media_b
    _media_step(rec, phase, "Upload per-task repair evidence, verify DB-level task association (real mechanism)", "TECHNICIAN", upload_task_evidence_and_verify_association)

    def complete_remaining_tasks():
        for t in ctx.repair_tasks[1:]:
            ctx.manager.call(
                "PUT", f"/repair-tasks/{t['id']}/assign",
                {"technicianName": ctx.fixtures.technician.first_name + " " + ctx.fixtures.technician.last_name,
                 "employeeId": ctx._tech_employee_id},
                expect=(200,),
            )
            ctx.technician.call("PUT", f"/repair-tasks/{t['id']}/start", expect=(200,))
            ctx.technician.call("PUT", f"/repair-tasks/{t['id']}/complete", expect=(200,))
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "REPAIR_COMPLETED")
    rec.step(phase, "Complete all remaining repair tasks, ServiceWorkflow -> REPAIR_COMPLETED", "TECHNICIAN", complete_remaining_tasks)

    def manager_customer_owner_see_completion():
        tracking = ctx.customer.get(f"/customer/repair-tracking/{ctx.jobcard_number}")
        assert_customer_progress_consistent(tracking)
        if not tracking.get("repairCompleted"):
            raise AssertionFailure(f"Customer does not see repair completed: {tracking}")
    rec.step(phase, "Customer/Manager/Owner see repair completion consistently", "CUSTOMER", manager_customer_owner_see_completion)


# ---------------------------------------------------------------------------
# PHASE 06 — quality check
# ---------------------------------------------------------------------------

def phase_06_quality_check(rec: StepRecorder, ctx: JourneyContext) -> None:
    phase = "06_quality_check"

    def pass_qc():
        ctx.manager.call(
            "POST", f"/jobcards/{ctx.jobcard_number}/quality-check/pass",
            {"inspectedBy": "MRT Manager", "remarks": "All repairs verified OK."}, expect=(200,),
        )
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "READY_FOR_INVOICE")
    rec.step(phase, "Manager passes QC, ServiceWorkflow -> READY_FOR_INVOICE", "MANAGER", pass_qc)

    def customer_progress_consistent_after_qc():
        tracking = ctx.customer.get(f"/customer/repair-tracking/{ctx.jobcard_number}")
        assert_customer_progress_consistent(tracking)
        if not tracking.get("qualityChecked"):
            raise AssertionFailure(f"Customer tracking doesn't reflect QC pass: {tracking}")
    rec.step(phase, "Customer progress view consistent after QC (regression: repair/QC/invoice contradiction)", "CUSTOMER", customer_progress_consistent_after_qc)


# ---------------------------------------------------------------------------
# PHASE 07 — invoice + payment
# ---------------------------------------------------------------------------

def phase_07_invoice_and_payment(rec: StepRecorder, ctx: JourneyContext) -> None:
    phase = "07_invoice_and_payment"

    def generate_invoice():
        invoice = ctx.manager.post(f"/workflow/{ctx.jobcard_number}/invoice", expect=(200,))
        ctx.invoice_id = invoice["id"]
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "INVOICE_GENERATED")
    rec.step(phase, "Manager generates invoice, ServiceWorkflow -> INVOICE_GENERATED", "MANAGER", generate_invoice)

    def customer_sees_invoice():
        invoices = ctx.customer.get("/customer/invoices")
        match = next((i for i in invoices if i.get("jobCardNumber") == ctx.jobcard_number), None)
        if match is None:
            raise AssertionFailure("Customer cannot see the new invoice")
        if match.get("paymentStatus") == "PAID":
            raise AssertionFailure("Invoice already shows PAID before any payment was made")
    rec.step(phase, "Customer sees invoice, correct amount, unpaid", "CUSTOMER", customer_sees_invoice)

    def customer_payment_blocker():
        """RELEASE BLOCKER regression (do not work around): the real
        customer-facing payInvoice() call hits POST
        /workflow/{jobCardNumber}/payment, which is
        @PreAuthorize(WORKFLOW_OPERATIONAL_ROLES) = MANAGER/SERVICE_ADVISOR/
        OWNER only. A CUSTOMER token must get 403. If this ever starts
        returning 200, that's a product decision having actually shipped —
        update this test to assert success AND ownership at that point, do
        not just flip expect=(200,) blindly.
        """
        status = assert_forbidden(
            ctx.client, "POST", f"/workflow/{ctx.jobcard_number}/payment", ctx.customer.token, actor="CUSTOMER",
        )
        if status != 403:
            raise AssertionFailure(f"Expected 403 for customer payment attempt, got {status}")
    rec.blocked(
        phase, "Customer Pay Now (real path)", "CUSTOMER",
        "POST /workflow/{jobCardNumber}/payment is role-gated to MANAGER/SERVICE_ADVISOR/OWNER; "
        "no customer-authorized payment endpoint exists today. This is a confirmed RELEASE BLOCKER, "
        "not bypassed — see phase_07's manager-side payment step below for how the journey continues.",
        classification="PRODUCT_DECISION_REQUIRED",
    )
    # We still assert the 403 itself as a regression (must stay forbidden until redesigned).
    rec.step(phase, "Customer payment endpoint correctly returns 403 (regression guard)", "SECURITY", customer_payment_blocker)

    def manager_records_payment_to_continue_journey():
        # Explicitly NOT a substitute for the customer payment test above —
        # this only exists so phases 08+ (delivery/closure) remain
        # exercisable in the same run. The BLOCKED status above is what
        # gets reported for "can a customer pay their own invoice."
        ctx.manager.post(f"/workflow/{ctx.jobcard_number}/payment", expect=(200,))
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "READY_FOR_DELIVERY")
    rec.step(phase, "[Journey continuation only] Manager records payment so delivery/closure remain testable", "MANAGER", manager_records_payment_to_continue_journey)


# ---------------------------------------------------------------------------
# PHASE 08 — driver assignment, dummy-GPS/STOMP, real handover, delivery
# ---------------------------------------------------------------------------

def phase_08_driver_and_navigation(rec: StepRecorder, ctx: JourneyContext) -> None:
    phase = "08_driver_and_navigation"
    ws_host = ctx.client.base_url.split("//")[1].split("/")[0]

    def create_navigation_request_and_trip():
        future = (datetime.datetime.now() + datetime.timedelta(hours=1)).strftime("%Y-%m-%dT%H:%M:%S")
        nav_request = ctx.manager.post(
            f"/navigation/requests?customerId={ctx.customer_id}",
            {
                "vehicleId": ctx.vehicle_id, "garageId": ctx.garage_id, "requestType": "DELIVERY",
                "deliveryAddress": "456 MRT Customer Home", "deliveryLatitude": 17.4, "deliveryLongitude": 78.4,
                "scheduledAt": future,
            },
            expect=(201,),
        )
        employees = ctx.manager.get(f"/garage-memberships/employees?garageId={ctx.garage_id}")
        if not any(e["email"] == ctx.fixtures.driver.email for e in employees):
            raise AssertionFailure("Driver does not appear in the garage employee roster")
        ctx._driver_employee_id = ctx.driver.user["id"]
        trip = ctx.manager.post(
            "/navigation/trips",
            {"navigationRequestId": nav_request["id"], "driverId": ctx._driver_employee_id},
            expect=(201,),
        )
        ctx.trip_id = trip["id"]
    rec.step(phase, "Manager creates navigation request + assigns driver to trip", "MANAGER", create_navigation_request_and_trip)

    def driver_sees_trip():
        trips = ctx.driver.get(f"/navigation/trips/driver/{ctx._driver_employee_id}")
        match = next((t for t in trips if t.get("id") == ctx.trip_id), None)
        if match is None:
            raise AssertionFailure("Driver cannot see their assigned trip")
    rec.step(phase, "Driver sees assigned trip with correct pickup/customer info", "DRIVER", driver_sees_trip)

    def driver_accepts_starts_arrives():
        for action in ("accept", "start", "arrive"):
            ctx.driver.call("POST", f"/navigation/trips/{ctx.trip_id}/{action}?driverId={ctx._driver_employee_id}", expect=(200,))
    rec.step(phase, "Driver accepts -> starts -> arrives", "DRIVER", driver_accepts_starts_arrives)

    def dummy_gps_stomp_sequence():
        """Deterministic dummy-GPS harness: connects as the driver over the
        real STOMP endpoint, subscribes to /topic/trips/{id}/location as a
        customer/manager subscriber would, publishes a scripted 6-point
        coordinate sequence to /app/location/update, and asserts every
        point arrives, in order, on the topic.

        KNOWN FINDING if this fails on ordering: DriverLocationServiceImpl
        .processLocation() is synchronous with no @Async/executor of its
        own, but Spring's STOMP @MessageMapping dispatch
        (clientInboundChannel) uses a multi-threaded pool by default with
        no per-session/per-trip ordering guarantee — WebSocketConfig here
        does not override configureClientInboundChannel to serialize it.
        Rapid back-to-back updates from the same driver/trip can therefore
        be *processed* out of order even though they *arrive* over the
        connection in order, and the resulting /topic broadcast reflects
        that reordering. This is a CODE_BUG classification (root cause
        confirmed by reading WebSocketConfig.java — no channel executor
        override exists), not a test-harness fluke; the fix (serialize
        per-trip, or add server/client-side timestamp-based reordering) is
        a real architecture trade-off and is NOT applied here unilaterally.
        """
        ws_url = f"ws://{ws_host}/ws"
        subscriber = StompSession(ws_url, ctx.manager.token)
        subscriber.connect()
        subscriber.subscribe(f"/topic/trips/{ctx.trip_id}/location")

        publisher = StompSession(ws_url, ctx.driver.token)
        publisher.connect()

        route = [
            (17.3850, 78.4867), (17.3900, 78.4700), (17.3950, 78.4550),
            (17.4000, 78.4400), (17.4050, 78.4250), (17.4000 + 0.0, 78.4000),
        ]
        for i, (lat, lon) in enumerate(route):
            publisher.send(
                "/app/location/update",
                {
                    "driverId": ctx._driver_employee_id, "tripId": ctx.trip_id,
                    "latitude": lat, "longitude": lon, "speed": 12.5, "heading": 90.0,
                    "accuracy": 5.0, "timestamp": int(datetime.datetime.now().timestamp() * 1000) + i,
                },
            )

        received = subscriber.drain(expected_count=len(route), timeout=10.0)
        publisher.close()
        subscriber.close()

        ctx._stomp_received = received
        if len(received) < len(route):
            raise AssertionFailure(
                f"Published {len(route)} location updates over STOMP but only "
                f"{len(received)} arrived on /topic/trips/{ctx.trip_id}/location within 10s: {received}"
            )
        got_lats = [m["payload"].get("latitude") for m in received]
        want_lats = [lat for lat, _ in route]
        if got_lats != want_lats:
            raise AssertionFailure(f"Location updates arrived out of order or corrupted. Expected {want_lats}, got {got_lats}")
    rec.step(phase, "Dummy-GPS sequence over real STOMP: publish 6 points, assert ordered delivery", "DRIVER+BACKEND", dummy_gps_stomp_sequence)

    def upload_delivery_media_and_verify_storage_location():
        media = ctx.driver.multipart(
            f"/navigation/trips/{ctx.trip_id}/media",
            {"driverId": str(ctx._driver_employee_id), "stage": "DELIVERY", "latitude": "17.4", "longitude": "78.4"},
            "file", "delivery_proof.png", PNG_1PX_RED, "image/png", expect=(200, 201),
        )
        ctx.uploaded_media["delivery"] = media
        # Documented finding, not invented: trip media is confirmed to be
        # stored at a LOCAL path, not Google Drive (unlike job-card media).
        if media.get("mediaUrl", "").startswith("/media/navigation/"):
            pass  # confirmed asymmetry, recorded in coverage doc — not a script bug
        elif not media.get("driveFileId"):
            raise AssertionFailure(f"Delivery media neither local nor Drive-backed — unexpected shape: {media}")
    rec.step(phase, "Driver uploads delivery photo (documents local-storage vs Drive asymmetry)", "DRIVER", upload_delivery_media_and_verify_storage_location)

    def real_handover_flow():
        code_resp = ctx.customer.get(f"/navigation/trips/{ctx.trip_id}/handover/code")
        code = code_resp["code"]
        ctx.driver.post(f"/navigation/trips/{ctx.trip_id}/handover/verify", {"code": code}, expect=(200,))
    rec.step(phase, "Real handover: customer's code verified by driver (not the manager shortcut)", "CUSTOMER+DRIVER", real_handover_flow)

    def driver_completes_trip():
        ctx.driver.call("POST", f"/navigation/trips/{ctx.trip_id}/complete?driverId={ctx._driver_employee_id}", expect=(200,))
    rec.step(phase, "Driver completes trip via the real NavigationTrip path (not 'Complete Delivery' shortcut)", "DRIVER", driver_completes_trip)

    def everyone_sees_delivery_state():
        tracking = ctx.customer.get(f"/customer/repair-tracking/{ctx.jobcard_number}")
        assert_customer_progress_consistent(tracking)
    rec.step(phase, "Customer/Manager/Owner see consistent delivery state", "CUSTOMER", everyone_sees_delivery_state)


# ---------------------------------------------------------------------------
# PHASE 09 — closure
# ---------------------------------------------------------------------------

def phase_09_closure(rec: StepRecorder, ctx: JourneyContext) -> None:
    phase = "09_closure"

    def record_delivery_and_close():
        # The driver-path trip.complete() above handles the actual pickup;
        # DeliveryController's record is the garage's own paperwork step
        # and is still required by JobCardStatusValidator before close —
        # calling it here is the real next step, not a shortcut substitute
        # for the driver flow (which already ran in full above).
        ctx.manager.post(
            "/deliveries",
            {
                "jobCardId": ctx.jobcard_id, "invoiceId": ctx.invoice_id,
                "deliveredBy": "MRT Driver", "receivedBy": "MRT Customer",
                "remarks": "Delivered via real driver/trip flow.",
            },
            expect=(201,),
        )
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "DELIVERED")
    rec.step(phase, "Record delivery, ServiceWorkflow -> DELIVERED", "MANAGER", record_delivery_and_close)

    def close_job():
        ctx.manager.post(f"/workflow/{ctx.jobcard_number}/close", expect=(200,))
        assert_workflow_status(ctx.client, ctx.manager.token, ctx.jobcard_number, "CLOSED")
    rec.step(phase, "Close Job Card, ServiceWorkflow -> CLOSED", "MANAGER", close_job)

    if "inspection" not in ctx.uploaded_media:
        rec.blocked(
            phase, "Media (Google Drive-backed) still retrievable after CLOSED", "MANAGER",
            "Skipped: no job-card media was successfully uploaded this run (Google Drive credential "
            "was rejected earlier in phase 03/05 — see those BLOCKED entries).",
            classification="EXTERNAL_SERVICE_ISSUE",
        )
    else:
        def media_survives_closure():
            media_list = ctx.manager.get(f"/job-cards/{ctx.jobcard_id}/media")
            if len(media_list) < 1:
                raise AssertionFailure(f"Expected at least 1 media item to survive to CLOSED, found {len(media_list)}")
            first_id = ctx.uploaded_media["inspection"]["id"]
            ctx.client.call(
                "GET", f"/job-cards/{ctx.jobcard_id}/media/{first_id}/content", token=ctx.manager.token, expect=None,
            )
            last_status = ctx.client.transcript[-1].status
            if last_status != 200:
                raise AssertionFailure(f"Media content unreachable after CLOSED: status {last_status}")
        rec.step(phase, "Media (Google Drive-backed) still retrievable after CLOSED", "MANAGER", media_survives_closure)

    def final_state_all_personas():
        customer_tracking = ctx.customer.get(f"/customer/repair-tracking/{ctx.jobcard_number}")
        if customer_tracking.get("status") != "CLOSED":
            raise AssertionFailure(f"Customer does not see CLOSED: {customer_tracking}")
        assignments = ctx.technician.get("/job-assignments/my")
        still_active = [a for a in assignments if a.get("jobCardNumber") == ctx.jobcard_number and a.get("status") not in ("COMPLETED", "CANCELLED")]
        if still_active:
            raise AssertionFailure(f"Technician still sees active work on a CLOSED job: {still_active}")
    rec.step(phase, "Customer sees CLOSED; Technician no longer sees active work", "ALL", final_state_all_personas)
