//package com.garageos.modules.jobcard.validator;
//
//import com.garageos.core.enums.JobCardStatus;
//import org.springframework.stereotype.Component;
//
//import java.util.EnumMap;
//import java.util.EnumSet;
//import java.util.Map;
//import java.util.Set;
//
//@Component
//public class JobCardStatusValidator {
//
//    private static final Map<JobCardStatus, Set<JobCardStatus>> VALID_TRANSITIONS =
//            new EnumMap<>(JobCardStatus.class);
//
//    static {
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.OPEN,
//                EnumSet.of(JobCardStatus.INSPECTION_PENDING)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.INSPECTION_PENDING,
//                EnumSet.of(JobCardStatus.INSPECTION_COMPLETED)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.INSPECTION_COMPLETED,
//                EnumSet.of(JobCardStatus.ESTIMATE_PENDING)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.ESTIMATE_PENDING,
//                EnumSet.of(JobCardStatus.WAITING_FOR_APPROVAL)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.WAITING_FOR_APPROVAL,
//                EnumSet.of(JobCardStatus.ESTIMATE_APPROVED)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.ESTIMATE_APPROVED,
//                EnumSet.of(JobCardStatus.REPAIR_IN_PROGRESS)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.REPAIR_IN_PROGRESS,
//                EnumSet.of(JobCardStatus.WORK_COMPLETED)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.WORK_COMPLETED,
//                EnumSet.of(JobCardStatus.QUALITY_CHECK)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.QUALITY_CHECK,
//                EnumSet.of(JobCardStatus.READY_FOR_DELIVERY)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.READY_FOR_DELIVERY,
//                EnumSet.of(JobCardStatus.INVOICE_GENERATED)
//        );
//
//        VALID_TRANSITIONS.put(
//                JobCardStatus.INVOICE_GENERATED,
//                EnumSet.of(JobCardStatus.PAYMENT_COMPLETED)
//        );
//        VALID_TRANSITIONS.put(
//                JobCardStatus.PAYMENT_COMPLETED,
//                EnumSet.of(JobCardStatus.DELIVERED)
//        );
//        VALID_TRANSITIONS.put(
//                JobCardStatus.DELIVERED,
//                EnumSet.of(JobCardStatus.CLOSED)
//        );
//    }
//
//    public void validate(JobCardStatus currentStatus,
//                         JobCardStatus targetStatus) {
//
//        Set<JobCardStatus> allowed =
//                VALID_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(JobCardStatus.class));
//
//        if (!allowed.contains(targetStatus)) {
//
//            throw new IllegalStateException(
//                    String.format(
//                            "Invalid Job Card status transition : %s -> %s",
//                            currentStatus,
//                            targetStatus
//                    )
//            );
//        }
//    }
//}

package com.garageos.modules.jobcard.validator;

import com.garageos.core.enums.JobCardStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Canonical GarageST JobCard Architecture v1 lifecycle:
 *
 * OPEN -> INSPECTION_PENDING -> INSPECTION_COMPLETED -> ESTIMATE_PENDING
 * -> WAITING_FOR_APPROVAL -> REPAIR_PENDING -> REPAIR_IN_PROGRESS
 * -> REPAIR_COMPLETED -> READY_FOR_INVOICE -> INVOICE_GENERATED
 * -> READY_FOR_DELIVERY -> DELIVERED -> CLOSED, with CANCELLED as a
 * terminal side-path from any pre-DELIVERED state.
 *
 * ESTIMATE_APPROVED, QUALITY_CHECK, WORK_COMPLETED, INVOICED,
 * PAYMENT_PENDING and PAYMENT_COMPLETED remain in the enum (never
 * deleted, never written by canonical operations) but are kept as
 * source-only entries below so a pre-existing JobCard row already
 * sitting in one of these legacy states can still advance forward
 * through canonical operations rather than becoming permanently stuck.
 * REPAIR_COMPLETED -> REPAIR_PENDING supports the QC-fail rework path
 * (QualityCheckServiceImpl.failQualityCheck).
 */
@Component
public class JobCardStatusValidator {

    private static final Map<JobCardStatus, Set<JobCardStatus>> VALID_TRANSITIONS =
            new EnumMap<>(JobCardStatus.class);

    static {

        VALID_TRANSITIONS.put(
                JobCardStatus.OPEN,
                EnumSet.of(
                        JobCardStatus.INSPECTION_PENDING,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.INSPECTION_PENDING,
                EnumSet.of(
                        JobCardStatus.INSPECTION_COMPLETED,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.INSPECTION_COMPLETED,
                EnumSet.of(
                        JobCardStatus.ESTIMATE_PENDING,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.ESTIMATE_PENDING,
                EnumSet.of(
                        JobCardStatus.WAITING_FOR_APPROVAL,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.WAITING_FOR_APPROVAL,
                EnumSet.of(
                        JobCardStatus.REPAIR_PENDING,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.REPAIR_PENDING,
                EnumSet.of(
                        JobCardStatus.REPAIR_IN_PROGRESS,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.REPAIR_IN_PROGRESS,
                EnumSet.of(
                        JobCardStatus.REPAIR_COMPLETED,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.REPAIR_COMPLETED,
                EnumSet.of(
                        JobCardStatus.READY_FOR_INVOICE,
                        JobCardStatus.REPAIR_PENDING,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.READY_FOR_INVOICE,
                EnumSet.of(
                        JobCardStatus.INVOICE_GENERATED,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.INVOICE_GENERATED,
                EnumSet.of(
                        JobCardStatus.READY_FOR_DELIVERY,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.READY_FOR_DELIVERY,
                EnumSet.of(
                        JobCardStatus.DELIVERED,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.DELIVERED,
                EnumSet.of(
                        JobCardStatus.CLOSED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.CLOSED,
                EnumSet.noneOf(JobCardStatus.class)
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.CANCELLED,
                EnumSet.noneOf(JobCardStatus.class)
        );

        // Legacy/non-canonical source states (never written by canonical
        // operations, kept only so a pre-existing row parked here can
        // still be moved forward by a canonical operation).
        VALID_TRANSITIONS.put(
                JobCardStatus.ESTIMATE_APPROVED,
                EnumSet.of(JobCardStatus.REPAIR_PENDING)
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.QUALITY_CHECK,
                EnumSet.of(
                        JobCardStatus.READY_FOR_INVOICE,
                        JobCardStatus.REPAIR_PENDING
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.WORK_COMPLETED,
                EnumSet.of(JobCardStatus.READY_FOR_INVOICE)
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.INVOICED,
                EnumSet.of(JobCardStatus.READY_FOR_DELIVERY)
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.PAYMENT_PENDING,
                EnumSet.of(JobCardStatus.READY_FOR_DELIVERY)
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.PAYMENT_COMPLETED,
                EnumSet.of(JobCardStatus.READY_FOR_DELIVERY)
        );
    }

    public void validate(JobCardStatus currentStatus,
                         JobCardStatus targetStatus) {

        Set<JobCardStatus> allowed =
                VALID_TRANSITIONS.getOrDefault(
                        currentStatus,
                        EnumSet.noneOf(JobCardStatus.class)
                );

        if (!allowed.contains(targetStatus)) {
            throw new IllegalStateException(
                    String.format(
                            "Invalid Job Card status transition: %s -> %s",
                            currentStatus,
                            targetStatus
                    )
            );
        }
    }
}