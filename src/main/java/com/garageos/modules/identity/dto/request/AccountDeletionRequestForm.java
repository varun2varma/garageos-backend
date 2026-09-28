package com.garageos.modules.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Bound from the plain HTML (application/x-www-form-urlencoded) form on the
 * public /delete-account page — not JSON, so this is consumed via
 * @ModelAttribute rather than @RequestBody. Same "any registered
 * identifier" shape as ForgotPasswordRequest.
 */
@Data
public class AccountDeletionRequestForm {

    @NotBlank(message = "Registered email or mobile number is required.")
    private String identifier;
}
