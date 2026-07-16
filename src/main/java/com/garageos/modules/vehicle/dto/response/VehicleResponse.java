package com.garageos.modules.vehicle.dto.response;

import lombok.Builder;
import lombok.Getter;

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
}