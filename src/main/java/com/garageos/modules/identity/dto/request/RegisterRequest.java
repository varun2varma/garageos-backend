package com.garageos.modules.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "First name is required.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    private String lastName;

    @NotBlank(message = "Mobile number is required.")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Invalid mobile number."
    )
    private String mobile;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email.")
    private String email;

    @NotBlank(message = "Username is required.")
    @Size(min = 4, max = 50)
    private String username;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 100)
    private String password;

}