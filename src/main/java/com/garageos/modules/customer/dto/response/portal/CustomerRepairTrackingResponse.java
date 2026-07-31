package com.garageos.modules.customer.dto.response.portal;

import com.garageos.core.enums.JobCardStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CustomerRepairTrackingResponse {

    private String jobCardNumber;

    private String registrationNumber;

    private JobCardStatus status;

    private LocalDate serviceDate;

    private LocalDate estimatedDeliveryDate;

    private Boolean inspectionCompleted;

    private Boolean estimatePrepared;

    private Boolean estimateApproved;

    private Boolean repairCompleted;

    private Boolean qualityChecked;

    private Boolean invoiceGenerated;

    private Boolean paymentCompleted;

}