package com.garageos.modules.vehiclemaster.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVehicleBrandRequest {

    @NotBlank
    private String name;

    private String country;

    private String logoUrl;

}