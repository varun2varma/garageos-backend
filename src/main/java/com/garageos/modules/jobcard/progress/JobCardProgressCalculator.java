package com.garageos.modules.jobcard.progress;

import com.garageos.core.enums.JobCardStatus;

import java.util.List;

/**
 * The single authoritative work-level progress model. Pure and stateless.
 *
 * <pre>
 *  Phase 1  Intake and estimate approval   0-30  (5 milestones x 6)
 *     intake recorded, inspection completed, estimate prepared,
 *     estimate submitted, customer approved
 *  Phase 2  Repair execution              30-70  (40 points over required RepairTasks)
 *     each non-cancelled task: assigned, accepted, started, completed = 1/4 of its share
 *  Phase 3  Quality, billing, fulfilment  70-100 (5 milestones x 6)
 *     quality check passed, invoice generated, payment OR active credit clearance,
 *     ready for delivery/collection, delivered AND closed
 * </pre>
 *
 * Progress is derived from persisted state only, so it is identical after refresh and across
 * personas. The Job Card status ladder is authoritative for milestones already passed: once the
 * status is at or beyond REPAIR_COMPLETED the whole repair phase is counted (even for historical
 * jobs with no task rows); task rows only refine progress while repair is still open. Credit
 * clearance is not payment, but both satisfy the same "cleared for release" milestone because the
 * backend only reaches READY_FOR_DELIVERY through ReleaseClearance. DELIVERED alone is 94;
 * only CLOSED reaches 100. CANCELLED is 0.
 */
public final class JobCardProgressCalculator {

    private static final int STEP = 6;
    private static final double REPAIR_POINTS = 40.0;

    private JobCardProgressCalculator() {
    }

    /** Lifecycle facts of one RepairTask. A cancelled task must be passed with cancelled=true. */
    public record TaskState(boolean cancelled, boolean assigned, boolean accepted, boolean started, boolean completed) {

        int milestones() {
            // Later milestones imply earlier ones (a completed task was necessarily started, accepted, assigned).
            if (completed) return 4;
            if (started) return 3;
            if (accepted) return 2;
            if (assigned) return 1;
            return 0;
        }
    }

    public record Result(int percent, int intakePoints, double repairPoints, int finalPoints, String phase) {
    }

    /** Position on the lifecycle ladder; the enum's declaration order is NOT lifecycle order. */
    static int rank(JobCardStatus s) {
        return switch (s) {
            case OPEN, INSPECTION_PENDING -> 0;
            case INSPECTION_COMPLETED -> 1;
            case ESTIMATE_PENDING -> 2;
            case WAITING_FOR_APPROVAL -> 3;
            case ESTIMATE_APPROVED, REPAIR_PENDING -> 4;
            case REPAIR_IN_PROGRESS -> 5;
            case REPAIR_COMPLETED, WORK_COMPLETED -> 6;
            case QUALITY_CHECK -> 7;
            case READY_FOR_INVOICE -> 8;
            case INVOICE_GENERATED, INVOICED, PAYMENT_PENDING -> 9;
            case PAYMENT_COMPLETED -> 10;
            case READY_FOR_DELIVERY -> 11;
            case DELIVERED -> 12;
            case CLOSED -> 13;
            case CANCELLED -> -1;
        };
    }

    public static Result compute(JobCardStatus status, List<TaskState> tasks) {

        if (status == null || status == JobCardStatus.CANCELLED) {
            return new Result(0, 0, 0, 0, "CANCELLED");
        }

        int r = rank(status);

        int intake = STEP;           // intake recorded
        if (r >= 1) intake += STEP;  // inspection completed
        if (r >= 2) intake += STEP;  // estimate prepared
        if (r >= 3) intake += STEP;  // estimate submitted
        if (r >= 4) intake += STEP;  // customer approved

        double repair;
        if (r >= 6) {
            repair = REPAIR_POINTS;
        } else if (r < 4) {
            repair = 0;
        } else {
            List<TaskState> required = tasks == null ? List.of() : tasks.stream().filter(t -> !t.cancelled()).toList();
            if (required.isEmpty()) {
                repair = 0; // nothing to execute yet: no credit for work that does not exist
            } else {
                double share = REPAIR_POINTS / required.size();
                repair = required.stream().mapToDouble(t -> share * t.milestones() / 4.0).sum();
            }
        }

        int fin = 0;
        if (r >= 8) fin += STEP;   // quality check passed
        if (r >= 9) fin += STEP;   // invoice generated
        if (r >= 10) fin += STEP;  // paid or credit-cleared
        if (r >= 11) fin += STEP;  // ready for delivery / collection
        if (r >= 13) fin += STEP;  // delivered and closed

        int percent = (int) Math.min(100, Math.round(intake + repair + fin));
        if (r >= 13) {
            percent = 100;
        } else if (percent >= 100) {
            percent = 99; // 100 is reserved for closed jobs
        }

        String phase = r >= 7 ? "FULFILMENT" : r >= 4 ? "REPAIR" : "INTAKE";
        if (r == 13) phase = "CLOSED";

        return new Result(percent, intake, repair, fin, phase);
    }
}
