package com.garageos.modules.places.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResult {

    private String displayName;
    private double latitude;
    private double longitude;
}
