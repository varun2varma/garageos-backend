package com.garageos.modules.vehicle.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVehicleRequest {

    @NotBlank(message = "Registration number is required.")
    private String registrationNumber;

    @NotBlank(message = "Brand is required.")
    private String brand;

    @NotBlank(message = "Model is required.")
    private String model;

    private String variant;

    private String fuelType;

    private String transmission;

    private Integer manufacturingYear;

    private String color;

    @NotNull(message = "Customer id is required.")
    private Long customerId;
}