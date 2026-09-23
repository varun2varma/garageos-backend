package com.garageos.modules.estimateitem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetSelectionRequest {

    @NotNull(message = "Selected is required.")
    private Boolean selected;
}
