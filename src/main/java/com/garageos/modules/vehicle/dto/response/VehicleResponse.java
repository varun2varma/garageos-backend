package com.garageos.modules.vehicle.dto.response;

import com.garageos.core.enums.vehicle.RcVerificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VehicleResponse {

    private Long id;

    private String registrationNumber;

    private String brand;

    private String model;

    private String variant;

    private String fuelType;

    private String transmission;

    private Integer manufacturingYear;

    private String color;

    private Long customerId;

    private String customerName;

    private String customerMobileNumber;

    private RcVerificationStatus rcVerificationStatus;

    private String rcDocumentReference;

    private Long rcVerifiedBy;

    private LocalDateTime rcVerifiedAt;
}