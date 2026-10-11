package com.garageos.modules.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkCatalogItemRequest {

    @NotNull
    private Long catalogItemId;
}
