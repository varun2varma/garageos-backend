package com.garageos.modules.garagemembership.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinGarageRequest {

    @NotBlank(message = "Garage code is required.")
    private String garageCode;

}