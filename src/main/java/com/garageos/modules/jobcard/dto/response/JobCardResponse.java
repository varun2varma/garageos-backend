package com.garageos.modules.jobcard.dto.response;

import com.garageos.modules.complaint.dto.response.ComplaintResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class JobCardResponse {

    private Long id;

    private String jobCardNumber;

    private Long customerId;

    private String customerName;

    private String customerMobileNumber;

    private Long vehicleId;

    private String registrationNumber;

    private String brand;

    private String model;

    private LocalDate serviceDate;

    private String status;

    private Long odometerReading;

    private LocalDate estimatedDeliveryDate;

    private List<ComplaintResponse> complaints;

    private String remarks;
}