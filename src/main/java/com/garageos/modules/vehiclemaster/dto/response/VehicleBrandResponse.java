package com.garageos.modules.vehiclemaster.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleBrandResponse {

    private Long id;

    private String name;

    private String country;

    private String logoUrl;

    private Boolean active;

}