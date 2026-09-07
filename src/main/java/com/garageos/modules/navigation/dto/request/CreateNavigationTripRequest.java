package com.garageos.modules.navigation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateNavigationTripRequest {

    @NotNull
    private Long navigationRequestId;

    @NotNull
    private Long driverId;

}
