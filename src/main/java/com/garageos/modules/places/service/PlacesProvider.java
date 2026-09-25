package com.garageos.modules.places.service;

import com.garageos.modules.places.dto.response.PlaceSearchResult;

import java.util.List;
import java.util.Optional;

/**
 * One place-search source (Google Places, Nominatim, ...) - mirrors the
 * navigation module's NavigationRouteProvider abstraction on purpose
 * (Google-primary/fallback pattern already established there for Routes).
 * Never throws for a search failure (bad response, timeout, no key
 * configured) - returns Optional.empty() so PlacesServiceImpl can fall
 * through to the next provider without a try/catch at every call site.
 * Optional.of(list) with an empty list is a genuine "no results" answer
 * from that provider, distinct from Optional.empty() (the provider itself
 * failed/is unavailable) - callers must not treat the two the same way.
 */
public interface PlacesProvider {

    Optional<List<PlaceSearchResult>> search(
            String query,
            Double biasLatitude,
            Double biasLongitude);
}
