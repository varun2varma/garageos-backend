package com.garageos.modules.places.service.impl;

import com.garageos.modules.places.dto.response.PlaceSearchResult;
import com.garageos.modules.places.service.PlacesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the two place-search providers: Google Places first when
 * configured, Nominatim as the fallback - never both in parallel, mirroring
 * NavigationRouteServiceImpl's Google-Routes/OSRM orchestration exactly.
 * Google is only skipped in favor of Nominatim when it returns
 * Optional.empty() (provider failure/unavailable) - a present-but-empty
 * result list from Google is treated as an authoritative "no results"
 * answer and returned as-is, not silently overridden by Nominatim.
 */
@Service
@RequiredArgsConstructor
public class PlacesServiceImpl implements PlacesService {

    private final GooglePlacesProvider googlePlacesProvider;
    private final NominatimPlacesProvider nominatimPlacesProvider;

    @Override
    public List<PlaceSearchResult> search(
            String query,
            Double biasLatitude,
            Double biasLongitude) {

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        if (googlePlacesProvider.isConfigured()) {
            Optional<List<PlaceSearchResult>> googleResult =
                    googlePlacesProvider.search(query.trim(), biasLatitude, biasLongitude);
            if (googleResult.isPresent()) {
                return googleResult.get();
            }
        }

        return nominatimPlacesProvider
                .search(query.trim(), biasLatitude, biasLongitude)
                .orElse(List.of());
    }

    @Override
    public Optional<PlaceSearchResult> details(String placeId) {
        return googlePlacesProvider.details(placeId);
    }
}
