"""Business-consequence assertions.

Rule: an HTTP 200 is never the assertion. Every helper here checks the
actual backend-state consequence of an action — what ServiceWorkflow
thinks the status is, what a *different* persona's own read endpoint
returns, or whether two supposedly-linked models agree with each other.
A helper that can't find a real consequence to check does not exist here;
see MASTER_E2E_COVERAGE.md for what is deliberately NOT_YET_AUTOMATED
instead of asserted-anyway.
"""
from __future__ import annotations

from .client import MasterE2EClient


class AssertionFailure(AssertionError):
    pass


def assert_workflow_status(client: MasterE2EClient, token: str, job_card_number: str, expected: str) -> dict:
    body = client.get(f"/workflow/{job_card_number}/status", token=token)
    actual = body.get("status")
    if actual != expected:
        raise AssertionFailure(
            f"ServiceWorkflow status for {job_card_number}: expected {expected!r}, got {actual!r}. "
            f"Full status payload: {body}"
        )
    return body


def assert_repair_task_and_assignment_agree(
    client: MasterE2EClient,
    manager_token: str,
    technician_token: str,
    job_card_number: str,
) -> None:
    """Permanent regression guard for the JobAssignment/RepairTask disconnect
    discovered in the real-device test (2026-09-18).

    The technician-facing Flutter screen (TechnicianDashboard) completes
    work through JobAssignmentService (/job-assignments/...). ServiceWorkflow
    and every manager/customer-facing screen read completion from RepairTask
    (/repair-tasks/... , /workflow/{n}/repair-tasks). If these two disagree,
    a technician's own "Complete" tap accomplishes nothing towards the real
    business workflow — this must fail loudly, not be silently tolerated.
    """
    repair_tasks = client.get(f"/workflow/{job_card_number}/repair-tasks", token=manager_token)
    assignments = client.get("/job-assignments/my", token=technician_token)

    assignments_by_task_desc = {a.get("serviceName"): a for a in assignments}

    mismatches = []
    for task in repair_tasks:
        desc = task.get("description")
        assignment = assignments_by_task_desc.get(desc)
        task_done = task.get("status") == "COMPLETED"
        assignment_done = assignment is not None and assignment.get("status") == "COMPLETED"
        if assignment_done and not task_done:
            mismatches.append(
                f"RepairTask {task.get('id')} ({desc!r}) is {task.get('status')!r} "
                f"but the matching JobAssignment shows COMPLETED — the technician's "
                f"own UI thinks this task is done; ServiceWorkflow does not."
            )

    if mismatches:
        raise AssertionFailure(
            "JobAssignment/RepairTask divergence detected (known architecture issue, "
            "see MASTER_E2E_COVERAGE.md):\n" + "\n".join(mismatches)
        )


def assert_media_belongs_to_task(media_item: dict, expected_repair_task_id: int | None) -> None:
    actual = media_item.get("repairTaskId")
    if actual != expected_repair_task_id:
        raise AssertionFailure(
            f"Media {media_item.get('id')} has repairTaskId={actual!r}, "
            f"expected {expected_repair_task_id!r} — task-level media association is wrong."
        )


def assert_media_not_leaked(media_list: list[dict], forbidden_job_card_id: int) -> None:
    leaked = [m for m in media_list if m.get("jobCardId") == forbidden_job_card_id]
    if leaked:
        raise AssertionFailure(
            f"{len(leaked)} media item(s) belonging to job card {forbidden_job_card_id} "
            f"leaked into a different job card's media list: {leaked}"
        )


def assert_customer_progress_consistent(tracking: dict) -> None:
    """A customer's `Invoice` step can never show done while `Quality check`
    doesn't — that would mean the app is telling the customer their invoice
    is ready before QC actually happened, which the backend's own state
    machine (JobCardStatusValidator) makes impossible. If the UI/tracking
    endpoint ever shows this, it's lying to the customer about sequence.
    """
    if tracking.get("invoiceGenerated") and not tracking.get("qualityChecked"):
        raise AssertionFailure(
            "Customer repair-tracking contradiction: invoiceGenerated=true but "
            f"qualityChecked=false. Full payload: {tracking}"
        )
    if tracking.get("qualityChecked") and not tracking.get("repairCompleted"):
        raise AssertionFailure(
            "Customer repair-tracking contradiction: qualityChecked=true but "
            f"repairCompleted=false. Full payload: {tracking}"
        )
    if tracking.get("paymentCompleted") and not tracking.get("invoiceGenerated"):
        raise AssertionFailure(
            "Customer repair-tracking contradiction: paymentCompleted=true but "
            f"invoiceGenerated=false. Full payload: {tracking}"
        )


def assert_forbidden(client: MasterE2EClient, method: str, path: str, token: str, body=None, actor: str = "") -> int:
    """Calls an endpoint that MUST reject the caller, and fails the test if it
    doesn't. Returns the actual status so the caller can log/report it.
    """
    status, parsed = _call_raw(client, method, path, body, token, actor)
    if status not in (401, 403, 404):
        raise AssertionFailure(
            f"{method} {path} was expected to be forbidden for this caller "
            f"but returned {status}: {str(parsed)[:300]}"
        )
    return status


def _call_raw(client: MasterE2EClient, method: str, path: str, body, token, actor):
    full = client.call(method, path, body, token=token, actor=actor, expect=None)
    last = client.transcript[-1]
    return last.status, full
