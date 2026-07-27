package com.garageos.modules.vehiclemaster.dto.response;

import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleVariantResponse {

    private Long id;

    private Long modelId;

    private String modelName;

    private String variantName;

    private FuelType fuelType;

    private TransmissionType transmissionType;

    private Integer engineCc;

    private Double horsepower;

    private Double torqueNm;

    private Integer launchYear;

    private Integer discontinuedYear;

    private Integer serviceIntervalKm;

    private Integer serviceIntervalMonths;

    private Boolean active;

}