package com.garageos.modules.places.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * latitude/longitude are nullable: a Google Autocomplete (New) suggestion
 * carries only placeId + displayName (no coordinates, per Google's own
 * cost-efficient guidance - Autocomplete is for the keystroke-by-keystroke
 * suggestion list, not for resolving coordinates on every suggestion).
 * The Flutter client resolves coordinates via GET /places/details?placeId=
 * only for the one suggestion the user actually selects. A Nominatim
 * result carries coordinates directly (placeId is null in that case) since
 * that API has no separate "details" step.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResult {

    private String placeId;
    private String displayName;
    private Double latitude;
    private Double longitude;
}
