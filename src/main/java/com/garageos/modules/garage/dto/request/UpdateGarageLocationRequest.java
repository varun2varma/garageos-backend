package com.garageos.modules.garage.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Dedicated add/edit-location request, separate from the full-object
 * `CreateGarageRequest` used by `PUT /garages/{id}` - lets the owner set or
 * change just the map location later, without resending the whole garage
 * profile. Both fields required here (an "add location" action is a
 * complete coordinate pair, never a partial update).
 */
@Data
public class UpdateGarageLocationRequest {

    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.")
    BigDecimal latitude;

    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.")
    BigDecimal longitude;
}
