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
                        JobCardStatus.WAITING_FOR_APPROVAL
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.WAITING_FOR_APPROVAL,
                EnumSet.of(
                        JobCardStatus.ESTIMATE_APPROVED,
                        JobCardStatus.CANCELLED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.ESTIMATE_APPROVED,
                EnumSet.of(
                        JobCardStatus.REPAIR_PENDING
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.REPAIR_PENDING,
                EnumSet.of(
                        JobCardStatus.REPAIR_IN_PROGRESS
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.REPAIR_IN_PROGRESS,
                EnumSet.of(
                        JobCardStatus.REPAIR_COMPLETED,
                        JobCardStatus.WORK_COMPLETED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.REPAIR_COMPLETED,
                EnumSet.of(
                        JobCardStatus.QUALITY_CHECK
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.WORK_COMPLETED,
                EnumSet.of(
                        JobCardStatus.QUALITY_CHECK
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.QUALITY_CHECK,
                EnumSet.of(
                        JobCardStatus.READY_FOR_INVOICE,
                        JobCardStatus.READY_FOR_DELIVERY
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.READY_FOR_INVOICE,
                EnumSet.of(
                        JobCardStatus.INVOICE_GENERATED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.INVOICE_GENERATED,
                EnumSet.of(
                        JobCardStatus.INVOICED,
                        JobCardStatus.PAYMENT_PENDING
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.INVOICED,
                EnumSet.of(
                        JobCardStatus.PAYMENT_PENDING
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.PAYMENT_PENDING,
                EnumSet.of(
                        JobCardStatus.PAYMENT_COMPLETED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.PAYMENT_COMPLETED,
                EnumSet.of(
                        JobCardStatus.READY_FOR_DELIVERY,
                        JobCardStatus.DELIVERED
                )
        );

        VALID_TRANSITIONS.put(
                JobCardStatus.READY_FOR_DELIVERY,
                EnumSet.of(
                        JobCardStatus.DELIVERED
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