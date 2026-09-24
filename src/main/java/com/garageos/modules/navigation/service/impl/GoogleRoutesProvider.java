package com.garageos.modules.navigation.service.impl;

import com.garageos.modules.navigation.dto.response.RouteResponse;
import com.garageos.modules.navigation.service.NavigationRouteProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Google Routes API (computeRoutes) as the primary route source (mission
 * Part Q). The server-restricted key configured via google.maps.routes.
 * api-key / GOOGLE_MAPS_ROUTES_API_KEY never leaves this backend -
 * unlike the previous Flutter-side GoogleRouteProvider, which sent it
 * directly from the client. Request/response shape (field mask, body,
 * encodedPolyline) is unchanged from that prior client-side
 * implementation, just moved server-side.
 */
@Slf4j
@Component
public class GoogleRoutesProvider implements NavigationRouteProvider {

    private static final String ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";

    @Value("${google.maps.routes.api-key:}")
    private String apiKey;

    private final RestClient restClient;

    public GoogleRoutesProvider() {
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
    public Optional<RouteResponse> getRoute(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude) {

        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = Map.of(
                    "origin", Map.of(
                            "location", Map.of(
                                    "latLng", Map.of(
                                            "latitude", originLatitude,
                                            "longitude", originLongitude))),
                    "destination", Map.of(
                            "location", Map.of(
                                    "latLng", Map.of(
                                            "latitude", destinationLatitude,
                                            "longitude", destinationLongitude))),
                    "travelMode", "DRIVE",
                    "routingPreference", "TRAFFIC_AWARE"
            );

            Map<String, Object> response = restClient.post()
                    .uri(ENDPOINT)
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask",
                            "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (routes == null || routes.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> route = routes.get(0);
            Map<String, Object> polyline = (Map<String, Object>) route.get("polyline");
            String encoded = polyline == null ? null : (String) polyline.get("encodedPolyline");
            if (encoded == null) {
                return Optional.empty();
            }

            String durationField = (String) route.get("duration");
            double durationSeconds = 0.0;
            if (durationField != null && durationField.endsWith("s")) {
                try {
                    durationSeconds = Double.parseDouble(durationField.substring(0, durationField.length() - 1));
                } catch (NumberFormatException ignored) {
                    durationSeconds = 0.0;
                }
            }

            Number distanceMeters = (Number) route.get("distanceMeters");

            return Optional.of(RouteResponse.builder()
                    .distanceMeters(distanceMeters == null ? 0.0 : distanceMeters.doubleValue())
                    .durationSeconds(durationSeconds)
                    .encodedPolyline(encoded)
                    .provider("GOOGLE")
                    .build());

        } catch (Exception e) {
            // Same fail-safe contract as every provider: never throw, never
            // fabricate a route - log for observability and let the caller
            // fall through to the next provider.
            log.warn("Google Routes API call failed, falling back: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
