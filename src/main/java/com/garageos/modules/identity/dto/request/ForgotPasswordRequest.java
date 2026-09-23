package com.garageos.modules.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    /** Username, email, or mobile — whichever the account was registered with. */
    @NotBlank(message = "Username, email, or mobile is required.")
    private String identifier;
}
