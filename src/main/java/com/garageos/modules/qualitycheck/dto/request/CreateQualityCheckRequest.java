package com.garageos.modules.qualitycheck.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQualityCheckRequest {

    /**
     * The garage employee who performed the check (required). Must be an active member of the job card's garage
     * holding a quality-check-eligible role (Owner, Manager, Service Advisor or Technician); a customer is never
     * eligible. The stored name is a snapshot taken from that employee, not from request text.
     */
    @jakarta.validation.constraints.NotNull(message = "Select the employee who performed the quality check.")
    private Long inspectedByUserId;

    /** Deprecated and ignored: kept so older clients that still send a name do not fail JSON binding. */
    @Deprecated
    private String inspectedBy;

    private String remarks;

}