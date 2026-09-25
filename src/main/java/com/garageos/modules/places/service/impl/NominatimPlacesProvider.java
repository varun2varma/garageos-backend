package com.garageos.modules.places.service.impl;

import com.garageos.modules.places.dto.response.PlaceSearchResult;
import com.garageos.modules.places.service.PlacesProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Nominatim/OSM as the fallback place-search source when Google Places is
 * not configured or fails - the same public endpoint/contract the Flutter
 * client's own (now-fallback-only) PlaceSearchService already called
 * directly, moved server-side so the fallback decision lives in one place
 * instead of being duplicated client- and server-side (mirrors
 * OsrmRoutesProvider's relationship to GoogleRoutesProvider for Routes).
 * Existing OSM usage-policy requirements (required User-Agent header,
 * public-instance rate limit) are preserved.
 */
@Slf4j
@Component
public class NominatimPlacesProvider implements PlacesProvider {

    private static final String BASE_URL = "https://nominatim.openstreetmap.org/search";

    private final RestClient restClient;

    public NominatimPlacesProvider() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<List<PlaceSearchResult>> search(
            String query,
            Double biasLatitude,
            Double biasLongitude) {

        try {
            String uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("q", query)
                    .queryParam("format", "jsonv2")
                    .queryParam("addressdetails", "0")
                    .queryParam("limit", "6")
                    .build()
                    .toUriString();

            List<Map<String, Object>> response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, "GarageST-Backend/1.0 (+https://garagest.example)")
                    .retrieve()
                    .body(List.class);

            if (response == null) {
                return Optional.of(List.of());
            }

            List<PlaceSearchResult> results = new ArrayList<>();

            for (Map<String, Object> entry : response) {

                Double lat = parseDouble(entry.get("lat"));
                Double lon = parseDouble(entry.get("lon"));
                String displayName = (String) entry.get("display_name");

                if (lat == null || lon == null
                        || displayName == null || displayName.isBlank()) {
                    continue;
                }

                results.add(PlaceSearchResult.builder()
                        .displayName(displayName.trim())
                        .latitude(lat)
                        .longitude(lon)
                        .build());
            }

            return Optional.of(results);

        } catch (Exception e) {
            log.warn("Nominatim place search failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
