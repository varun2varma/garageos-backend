package com.garageos.modules.places.service;

import com.garageos.modules.places.dto.response.PlaceSearchResult;

import java.util.List;

public interface PlacesService {

    List<PlaceSearchResult> search(
            String query,
            Double biasLatitude,
            Double biasLongitude);
}
