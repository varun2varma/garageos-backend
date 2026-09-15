package com.garageos.modules.media.dto.request;

import com.garageos.core.enums.media.MediaVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for the media-visibility-correction endpoint. Deliberately
 * carries only the one, enum-typed field — no generic/free-text visibility
 * field exists anywhere in this request, so there is no way to bypass the
 * CUSTOMER_VISIBLE/INTERNAL restriction from the API.
 */
@Getter
@Setter
public class UpdateMediaVisibilityRequest {

    @NotNull
    private MediaVisibility visibility;
}
