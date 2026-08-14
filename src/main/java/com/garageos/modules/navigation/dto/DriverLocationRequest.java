package com.garageos.modules.navigation.dto;

import lombok.Data;

@Data
public class DriverLocationRequest {

    private Long driverId;

    private Long tripId;

    private Double latitude;

    private Double longitude;

    private Double speed;

    private Double heading;

    private Double accuracy;

    private Long timestamp;
}