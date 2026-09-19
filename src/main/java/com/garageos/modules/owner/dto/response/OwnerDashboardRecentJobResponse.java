package com.garageos.modules.owner.dto.response;

import com.garageos.core.enums.JobCardStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class OwnerDashboardRecentJobResponse {

    private Long jobCardId;

    private String jobCardNumber;

    private String customerName;

    private String mobileNumber;

    private String registrationNumber;

    private String vehicleName;

    private JobCardStatus status;

    private LocalDate serviceDate;

    private Long odometerReading;

    private LocalDate estimatedDeliveryDate;
}