# Job Card work-level progress (authoritative)

Source: `modules/jobcard/progress/JobCardProgressCalculator` (pure) + `JobCardProgressService` (loads RepairTasks).
Consumers: `JobCardProjectionServiceImpl` (job card view) and `ServiceWorkflowServiceImpl` (workflow status).
The two old, disagreeing status→% tables were removed; Flutter only renders the `progress` value it receives.

| Phase | Range | Milestones (6 pts each unless noted) |
|---|---|---|
| Intake + approval | 0–30 | intake recorded; inspection completed; estimate prepared; estimate submitted; customer approved |
| Repair | 30–70 | 40 pts split equally over non-cancelled RepairTasks; each task: assigned, accepted, started, completed = ¼ each |
| Quality / billing / fulfilment | 70–100 | quality passed; invoice generated; paid **or** credit-cleared; ready for delivery; delivered **and** closed |

Rules
- Status ladder is authoritative for milestones already passed; at or beyond REPAIR_COMPLETED the whole 40 pts count (covers historical jobs with no task rows). Task rows only refine progress while repair is open.
- No tasks / all cancelled while repair is open → 0 repair points. Cancelled tasks never count as completed. Tasks added later dilute the per-task share.
- Credit is not payment, but both satisfy the single "cleared for release" milestone; READY_FOR_DELIVERY is only reachable through `ReleaseClearance`.
- DELIVERED = 94; only CLOSED = 100 (previously DELIVERED/INVOICED/PAYMENT_PENDING/WORK_COMPLETED fell to the default and showed 0%). CANCELLED = 0.
- QUALITY_CHECK (in progress) plateaus at 70 with REPAIR_COMPLETED.

Examples: two tasks, A completed, B accepted at REPAIR_IN_PROGRESS → 30 + 20 + 10 = 60. Approved, 2 tasks, one assigned → 35.

Open business questions: whether DELIVERED (not yet closed) should read 100; whether a failed QC returning a job to repair may lower progress (the calculator is derived, so it would).
Tests: `JobCardProgressCalculatorTest` (10). Not verified: end-to-end with a real database; physical device.

## Audit: old vs new (2026-10-18)
Old: **two** backend status→% tables, both status-only, no RepairTask input, and they disagreed for the same job
(`JobCardProjectionServiceImpl.resolveProgress` e.g. REPAIR_IN_PROGRESS 60, DELIVERED 95; `ServiceWorkflowServiceImpl.resolveProgress` 70, DELIVERED/INVOICED/PAYMENT_PENDING/WORK_COMPLETED fell to `default -> 0`).
Plus a third copy in Flutter, `approximateJobCardProgress`, mirroring the projection table (fallback only when `view.progress` is null).
Flow: `GET /jobcards/{id}/view` → `JobCardViewResponse.progress` → `JobCardService` (Flutter) → `job_card_detail_screen`; `GET /workflow/{number}/status` → `WorkflowStatusResponse.progress` → `workflow_shell_screen` and the legacy `employee/pages/workflow.js`.
New: both endpoints call `JobCardProgressService`; API field names/types unchanged (int). Flutter fallback table re-aligned to the calculator's no-task floor.
Not percentages (left alone): `CustomerPortalServiceImpl.LIFECYCLE_RANK` (boolean milestones for repair tracking), `repair_screen`/`technician_active_job_screen` bars (completed-task ratio inside one screen), Owner pipeline stage counts.
Known duplication: three lifecycle ladders exist (`JobCardStatusValidator`, `CustomerPortalServiceImpl.LIFECYCLE_RANK`, the calculator's `rank`); unifying them is a follow-up refactor, not done to avoid changing customer tracking behaviour.
Closure invariants: `JobCardServiceImpl.requireClosurePrerequisites` already requires a Delivery record and a generated invoice that is paid or Owner-credit-cleared, so CLOSED => 100 is backed by the backend, not hardcoded.
Tests: `JobCardProgressCalculatorTest` (10), `JobCardProgressServiceTest` (3; row→milestone mapping). Not covered: HTTP-level controller test, historical rows with inconsistent timestamps beyond "completedAt wins".
