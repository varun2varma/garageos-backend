package com.garageos.modules.vehiclemaster.dto.request;

import com.garageos.modules.vehiclemaster.enums.BodyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVehicleModelRequest {

    @NotNull
    private Long brandId;

    @NotBlank
    private String name;

    private BodyType bodyType;

    private Integer seatingCapacity;

}