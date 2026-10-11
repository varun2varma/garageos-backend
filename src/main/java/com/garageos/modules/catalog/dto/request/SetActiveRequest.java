package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetActiveRequest {

    @NotNull
    private Boolean active;

    /** The version the administrator loaded; a stale value is rejected. */
    private Long version;
}
