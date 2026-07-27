package com.garageos.modules.vehiclemaster.dto.request;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVehicleVariantRequest {

    @NotNull
    private Long modelId;

    @NotBlank
    private String variantName;

    @NotNull
    private FuelType fuelType;

    @NotNull
    private TransmissionType transmissionType;

    private Integer engineCc;

    private Double horsepower;

    private Double torqueNm;

    private Integer launchYear;

    private Integer discontinuedYear;

    private Integer serviceIntervalKm;

    private Integer serviceIntervalMonths;

}