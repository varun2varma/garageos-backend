package com.garageos.modules.customer.dto.request.portal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Mission backlog #14 — customer self-service profile edit. Deliberately
 * excludes mobileNumber: that field is the customer's login/lookup
 * identity throughout this codebase (see
 * CustomerPortalServiceImpl.getCurrentCustomer's findByMobileNumber
 * pattern, used everywhere) - changing it here would be a session/identity
 * semantics change, out of scope for a profile-fields edit.
 */
@Data
public class UpdateCustomerProfileRequest {

    @NotBlank(message = "First name is required.")
    private String firstName;

    private String lastName;

    @Email(message = "Please enter a valid email address.")
    private String email;

    private String address;

    private String city;

    private String state;

    private String pincode;
}
