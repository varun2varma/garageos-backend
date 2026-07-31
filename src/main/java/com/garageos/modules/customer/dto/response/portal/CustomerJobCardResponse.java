package com.garageos.modules.customer.dto.response.portal;

import com.garageos.core.enums.JobCardStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CustomerJobCardResponse {

    private Long id;

    private String jobCardNumber;

    private String registrationNumber;

    private LocalDate serviceDate;

    private LocalDate estimatedDeliveryDate;

    private Long odometerReading;

    private JobCardStatus status;

}