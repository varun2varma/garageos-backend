package com.garageos.modules.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Mission backlog #14 — employee self-service profile edit (PUT
 * /auth/me), distinct from the admin-facing PUT /users/{id}
 * (UpdateUserRequest, which also carries active/roleIds — administrative
 * fields a self-edit must never expose). Deliberately excludes username,
 * mobile and roles/active/garage membership — those are identity/
 * authorization fields granted by an Owner/Manager through the
 * employee-approval flow, not self-editable (see
 * EmployeeProfileScreen's own existing doc comment on the Flutter side).
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "First name is required.")
    private String firstName;

    private String lastName;

    @Email(message = "Please enter a valid email address.")
    private String email;
}
