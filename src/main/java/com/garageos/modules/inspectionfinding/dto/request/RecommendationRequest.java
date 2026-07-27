package com.garageos.modules.inspectionfinding.dto.request;


import com.garageos.core.enums.FuelType;
import com.garageos.core.enums.TransmissionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationRequest {

    private String brand;

    private String model;

    private String variant;

    private FuelType fuelType;

    private TransmissionType transmission;

    private Integer manufacturingYear;

    private Integer odometer;
}