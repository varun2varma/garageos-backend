package com.garageos.modules.places.service.impl;

import com.garageos.modules.places.dto.response.PlaceSearchResult;
import com.garageos.modules.places.service.PlacesProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Google Places API (New) Text Search as the primary place-search source,
 * mirroring GoogleRoutesProvider's pattern for Routes: the server-restricted
 * key configured via google.maps.places.api-key / GOOGLE_MAPS_PLACES_API_KEY
 * never leaves this backend - the Flutter client never sees it. Kept as a
 * separate credential/config key from google.maps.routes.api-key (see
 * application.properties) since Places and Routes are separate Google Cloud
 * API restrictions.
 */
@Slf4j
@Component
public class GooglePlacesProvider implements PlacesProvider {

    private static final String ENDPOINT = "https://places.googleapis.com/v1/places:searchText";

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
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("textQuery", query);

            if (biasLatitude != null && biasLongitude != null) {
                body.put("locationBias", Map.of(
                        "circle", Map.of(
                                "center", Map.of(
                                        "latitude", biasLatitude,
                                        "longitude", biasLongitude),
                                "radius", 50000.0)));
            }

            Map<String, Object> response = restClient.post()
                    .uri(ENDPOINT)
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask",
                            "places.displayName,places.formattedAddress,places.location")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> places = (List<Map<String, Object>>) response.get("places");
            if (places == null) {
                return Optional.of(List.of());
            }

            List<PlaceSearchResult> results = new ArrayList<>();

            for (Map<String, Object> place : places) {

                Map<String, Object> location = (Map<String, Object>) place.get("location");
                if (location == null) {
                    continue;
                }

                Number lat = (Number) location.get("latitude");
                Number lng = (Number) location.get("longitude");
                if (lat == null || lng == null) {
                    continue;
                }

                String displayName = extractDisplayName(place);
                if (displayName == null || displayName.isBlank()) {
                    continue;
                }

                results.add(PlaceSearchResult.builder()
                        .displayName(displayName)
                        .latitude(lat.doubleValue())
                        .longitude(lng.doubleValue())
                        .build());
            }

            return Optional.of(results);

        } catch (Exception e) {
            // Same fail-safe contract as every provider in this codebase:
            // never throw, never fabricate a result - log for
            // observability and let the caller fall through to the next
            // provider.
            log.warn("Google Places API call failed, falling back: {}", e.getMessage());
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
