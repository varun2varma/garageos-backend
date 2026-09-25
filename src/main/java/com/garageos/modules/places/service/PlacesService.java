package com.garageos.modules.places.service;

import com.garageos.modules.places.dto.response.PlaceSearchResult;

import java.util.List;
import java.util.Optional;

public interface PlacesService {

    List<PlaceSearchResult> search(
            String query,
            Double biasLatitude,
            Double biasLongitude);

    /**
     * Resolves a Google placeId (from a prior search() suggestion) to its
     * coordinates. Google-only - a Nominatim result never has a placeId
     * since it already carries coordinates inline.
     */
    Optional<PlaceSearchResult> details(String placeId);
}
