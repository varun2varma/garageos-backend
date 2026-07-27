package com.garageos.modules.vehiclemaster.dto.response;

import com.garageos.modules.vehiclemaster.enums.BodyType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleModelResponse {

    private Long id;

    private Long brandId;

    private String brandName;

    private String name;

    private BodyType bodyType;

    private Integer seatingCapacity;

    private Boolean active;

}