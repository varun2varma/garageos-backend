package com.garageos.modules.inspectionfinding.dto.request;

import lombok.Data;

@Data
public class RecommendationRequest {

    private Long vehicleId;

    private Integer odometer;

}