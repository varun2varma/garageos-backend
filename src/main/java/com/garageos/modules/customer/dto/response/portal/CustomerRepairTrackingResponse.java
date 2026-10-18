package com.garageos.modules.customer.dto.response.portal;

import com.garageos.core.enums.JobCardStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CustomerRepairTrackingResponse {

    /** The caller's own job card id (the customer app opens the service report with it). */
    private Long jobCardId;

    private String jobCardNumber;

    private String registrationNumber;

    private JobCardStatus status;

    private LocalDate serviceDate;

    private LocalDate estimatedDeliveryDate;

    /** Authoritative 0-100 work progress (same value every persona sees); null only if it could not be computed. */
    private Integer progress;

    private Boolean inspectionCompleted;

    private Boolean estimatePrepared;

    private Boolean estimateApproved;

    private Boolean repairCompleted;

    private Boolean qualityChecked;

    private Boolean invoiceGenerated;

    private Boolean paymentCompleted;

    /** Id of this job card's estimate (the caller's own); null until one exists. Lets the app open it directly. */
    private Long estimateId;

    /** Id of this job card's invoice; null until generated. Details stay viewable while unpaid. */
    private Long invoiceId;

    /** Invoice lifecycle / payment state names (e.g. GENERATED, PENDING, PARTIALLY_PAID, PAID); null without an invoice. */
    private String invoiceStatus;
    private String invoicePaymentStatus;

    /** True only when the invoice is fully PAID - the sole condition under which the invoice PDF is issued. */
    private Boolean invoicePdfAvailable;

}