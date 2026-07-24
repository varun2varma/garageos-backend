package com.garageos.modules.inspectionmaster.dto.request;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInspectionMasterRequest {

    @NotBlank
    private String make;

    @NotBlank
    private String model;

    private String variant;

    @NotNull
    private FuelType fuelType;

    @NotNull
    private TransmissionType transmissionType;

    @Min(1900)
    private Integer minYear;

    @Min(1900)
    private Integer maxYear;

    @NotNull
    @Min(0)
    private Integer minOdometer;

    @NotNull
    @Min(0)
    private Integer maxOdometer;

}