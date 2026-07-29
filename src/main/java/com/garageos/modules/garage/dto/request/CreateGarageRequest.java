package com.garageos.modules.garage.dto.request;

import com.garageos.core.enums.garage.WorkshopType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateGarageRequest {

    @NotBlank(message = "Garage name is required.")
    String garageName;

    @NotNull(message = "Workshop type is required.")
    WorkshopType workshopType;

    @NotNull(message = "Number of bays is required.")
    @Min(value = 1, message = "Number of bays must be at least 1.")
    Integer numberOfBays;

    @NotBlank(message = "Address is required.")
    String address;

    String landmark;

    @NotBlank(message = "City is required.")
    String city;

    @NotBlank(message = "State is required.")
    String state;

    @Pattern(
            regexp = "^\\d{6}$",
            message = "Pincode must be 6 digits."
    )
    String pincode;

    @Pattern(
            regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$",
            message = "Invalid GST Number."
    )
    String gstNumber;

    @Pattern(
            regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$",
            message = "Invalid PAN Number."
    )
    String panNumber;

}