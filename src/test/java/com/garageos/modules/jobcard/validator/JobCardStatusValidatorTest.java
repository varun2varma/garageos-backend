package com.garageos.modules.jobcard.validator;

import com.garageos.core.enums.JobCardStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Locks in the canonical GarageST JobCard Architecture v1 lifecycle table.
 * Of particular importance: INVOICE_GENERATED -> READY_FOR_DELIVERY is now
 * legal (it previously was not, which was the confirmed, source-verified
 * contradiction between the validator and the live
 * generateWorkflowInvoice()/receivePayment() client sequence).
 */
class JobCardStatusValidatorTest {

    private final JobCardStatusValidator validator = new JobCardStatusValidator();

    @Test
    void fullCanonicalHappyPath_isValid() {

        assertThatCode(() -> {
            validator.validate(JobCardStatus.OPEN, JobCardStatus.INSPECTION_PENDING);
            validator.validate(JobCardStatus.INSPECTION_PENDING, JobCardStatus.INSPECTION_COMPLETED);
            validator.validate(JobCardStatus.INSPECTION_COMPLETED, JobCardStatus.ESTIMATE_PENDING);
            validator.validate(JobCardStatus.ESTIMATE_PENDING, JobCardStatus.WAITING_FOR_APPROVAL);
            validator.validate(JobCardStatus.WAITING_FOR_APPROVAL, JobCardStatus.REPAIR_PENDING);
            validator.validate(JobCardStatus.REPAIR_PENDING, JobCardStatus.REPAIR_IN_PROGRESS);
            validator.validate(JobCardStatus.REPAIR_IN_PROGRESS, JobCardStatus.REPAIR_COMPLETED);
            validator.validate(JobCardStatus.REPAIR_COMPLETED, JobCardStatus.READY_FOR_INVOICE);
            validator.validate(JobCardStatus.READY_FOR_INVOICE, JobCardStatus.INVOICE_GENERATED);
            validator.validate(JobCardStatus.INVOICE_GENERATED, JobCardStatus.READY_FOR_DELIVERY);
            validator.validate(JobCardStatus.READY_FOR_DELIVERY, JobCardStatus.DELIVERED);
            validator.validate(JobCardStatus.DELIVERED, JobCardStatus.CLOSED);
        }).doesNotThrowAnyException();
    }

    @Test
    void qcFailRework_returnsToRepairPending() {

        assertThatCode(() ->
                validator.validate(JobCardStatus.REPAIR_COMPLETED, JobCardStatus.REPAIR_PENDING)
        ).doesNotThrowAnyException();
    }

    @Test
    void cancellation_isValidFromPreDeliveryStates() {

        assertThatCode(() -> {
            validator.validate(JobCardStatus.OPEN, JobCardStatus.CANCELLED);
            validator.validate(JobCardStatus.REPAIR_IN_PROGRESS, JobCardStatus.CANCELLED);
            validator.validate(JobCardStatus.READY_FOR_DELIVERY, JobCardStatus.CANCELLED);
        }).doesNotThrowAnyException();
    }

    @Test
    void cancellation_isRejectedAfterDelivered() {

        assertThatThrownBy(() ->
                validator.validate(JobCardStatus.DELIVERED, JobCardStatus.CANCELLED)
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void closed_isTerminal_noFurtherTransitions() {

        assertThatThrownBy(() ->
                validator.validate(JobCardStatus.CLOSED, JobCardStatus.CANCELLED)
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void closingBeforeDelivered_isRejected() {

        assertThatThrownBy(() ->
                validator.validate(JobCardStatus.READY_FOR_DELIVERY, JobCardStatus.CLOSED)
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nonCanonicalLegacyStatesAreNeverTargets() {

        // ESTIMATE_APPROVED, QUALITY_CHECK, WORK_COMPLETED, INVOICED,
        // PAYMENT_PENDING and PAYMENT_COMPLETED must never appear as a
        // valid TARGET of any transition in the canonical table.
        for (JobCardStatus from : JobCardStatus.values()) {
            for (JobCardStatus legacy : new JobCardStatus[]{
                    JobCardStatus.ESTIMATE_APPROVED,
                    JobCardStatus.QUALITY_CHECK,
                    JobCardStatus.WORK_COMPLETED,
                    JobCardStatus.INVOICED,
                    JobCardStatus.PAYMENT_PENDING,
                    JobCardStatus.PAYMENT_COMPLETED
            }) {
                assertThatThrownBy(() -> validator.validate(from, legacy))
                        .as("%s -> %s must be rejected".formatted(from, legacy))
                        .isInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    void legacySourceStates_canStillAdvanceForward() {

        // Backward compatibility: a pre-existing row parked in a legacy
        // state is not permanently stuck.
        assertThatCode(() -> {
            validator.validate(JobCardStatus.ESTIMATE_APPROVED, JobCardStatus.REPAIR_PENDING);
            validator.validate(JobCardStatus.WORK_COMPLETED, JobCardStatus.READY_FOR_INVOICE);
            validator.validate(JobCardStatus.INVOICED, JobCardStatus.READY_FOR_DELIVERY);
            validator.validate(JobCardStatus.PAYMENT_PENDING, JobCardStatus.READY_FOR_DELIVERY);
            validator.validate(JobCardStatus.PAYMENT_COMPLETED, JobCardStatus.READY_FOR_DELIVERY);
        }).doesNotThrowAnyException();
    }
}
