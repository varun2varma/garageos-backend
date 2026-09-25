package com.garageos.modules.places.service.impl;

import com.garageos.modules.places.dto.response.PlaceSearchResult;
import com.garageos.modules.places.service.PlacesProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Google Places API (New) as the primary place-search source, mirroring
 * GoogleRoutesProvider's pattern for Routes: the server-restricted key
 * configured via google.maps.places.api-key / GOOGLE_MAPS_PLACES_API_KEY
 * never leaves this backend - the Flutter client never sees it. Kept as a
 * separate credential/config key from google.maps.routes.api-key since
 * Places and Routes are separate Google Cloud API restrictions.
 *
 * Uses Autocomplete (New), not Text Search, for the keystroke-by-keystroke
 * suggestion list - Autocomplete is purpose-built for partial/prefix input
 * ("solitaire abod" -> the same result "solitaire abode" would return),
 * whereas Text Search is a relevance-ranked full search that can require a
 * more complete query to score a match highly enough to surface. Cost
 * control per Google's own guidance: Autocomplete returns only placeId +
 * display text (no coordinates) for every suggestion; coordinates are only
 * resolved via {@link #details(String)} for the one suggestion the user
 * actually selects, via a separate, minimally-field-masked Place Details
 * call - not for every keystroke's suggestion list.
 */
@Slf4j
@Component
public class GooglePlacesProvider implements PlacesProvider {

    private static final String AUTOCOMPLETE_ENDPOINT = "https://places.googleapis.com/v1/places:autocomplete";
    private static final String DETAILS_ENDPOINT = "https://places.googleapis.com/v1/places/";

    @Value("${google.maps.places.api-key:}")
    private String apiKey;

    private final RestClient restClient;

    public GooglePlacesProvider() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<List<PlaceSearchResult>> search(
            String query,
            Double biasLatitude,
            Double biasLongitude) {

        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("input", query);

            if (biasLatitude != null && biasLongitude != null) {
                body.put("locationBias", Map.of(
                        "circle", Map.of(
                                "center", Map.of(
                                        "latitude", biasLatitude,
                                        "longitude", biasLongitude),
                                "radius", 50000.0)));
            }

            Map<String, Object> response = restClient.post()
                    .uri(AUTOCOMPLETE_ENDPOINT)
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask",
                            "suggestions.placePrediction.placeId,suggestions.placePrediction.text.text")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> suggestions = (List<Map<String, Object>>) response.get("suggestions");
            if (suggestions == null) {
                return Optional.of(List.of());
            }

            List<PlaceSearchResult> results = new ArrayList<>();

            for (Map<String, Object> suggestion : suggestions) {

                Map<String, Object> prediction = (Map<String, Object>) suggestion.get("placePrediction");
                if (prediction == null) {
                    continue;
                }

                String placeId = (String) prediction.get("placeId");
                Map<String, Object> text = (Map<String, Object>) prediction.get("text");
                String displayName = text == null ? null : (String) text.get("text");

                if (placeId == null || displayName == null || displayName.isBlank()) {
                    continue;
                }

                // No coordinates here by design - see class doc comment.
                // The caller resolves them via details(placeId) only for
                // the suggestion actually selected.
                results.add(PlaceSearchResult.builder()
                        .placeId(placeId)
                        .displayName(displayName.trim())
                        .build());
            }

            return Optional.of(results);

        } catch (Exception e) {
            // Same fail-safe contract as every provider in this codebase:
            // never throw, never fabricate a result - log for
            // observability and let the caller fall through to the next
            // provider.
            log.warn("Google Places Autocomplete call failed, falling back: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Resolves one placeId (from a prior {@link #search} suggestion) to its
     * coordinates + display info via Place Details, with a minimal field
     * mask (displayName, formattedAddress, location only) per Google's own
     * cost-control guidance - never the full Place Details payload.
     */
    @SuppressWarnings("unchecked")
    public Optional<PlaceSearchResult> details(String placeId) {

        if (!isConfigured() || placeId == null || placeId.isBlank()) {
            return Optional.empty();
        }

        try {
            String uri = UriComponentsBuilder
                    .fromUriString(DETAILS_ENDPOINT + placeId)
                    .toUriString();

            Map<String, Object> place = restClient.get()
                    .uri(uri)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "displayName,formattedAddress,location")
                    .retrieve()
                    .body(Map.class);

            if (place == null) {
                return Optional.empty();
            }

            Map<String, Object> location = (Map<String, Object>) place.get("location");
            if (location == null) {
                return Optional.empty();
            }

            Number lat = (Number) location.get("latitude");
            Number lng = (Number) location.get("longitude");
            if (lat == null || lng == null) {
                return Optional.empty();
            }

            String displayName = extractDisplayName(place);
            if (displayName == null || displayName.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(PlaceSearchResult.builder()
                    .placeId(placeId)
                    .displayName(displayName)
                    .latitude(lat.doubleValue())
                    .longitude(lng.doubleValue())
                    .build());

        } catch (Exception e) {
            log.warn("Google Place Details call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractDisplayName(Map<String, Object> place) {

        Map<String, Object> displayName = (Map<String, Object>) place.get("displayName");
        String name = displayName == null ? null : (String) displayName.get("text");
        String formattedAddress = (String) place.get("formattedAddress");

        if (name != null && formattedAddress != null && !formattedAddress.isBlank()) {
            return name + ", " + formattedAddress;
        }

        if (formattedAddress != null && !formattedAddress.isBlank()) {
            return formattedAddress;
        }

        return name;
    }
}
