package com.garageos.modules.navigation.service.impl;

import com.garageos.modules.navigation.dto.response.RouteResponse;
import com.garageos.modules.navigation.service.NavigationRouteProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * OSRM's public driving-profile API as the fallback route source (mission
 * Part R) when Google Routes is not configured or fails - the same public
 * endpoint/contract the Flutter client's own (now-retired-as-primary)
 * OsrmRouteProvider already called, moved server-side so the fallback
 * decision lives in one place instead of being duplicated client- and
 * server-side.
 */
@Slf4j
@Component
public class OsrmRoutesProvider implements NavigationRouteProvider {

    private static final String BASE_URL = "https://router.project-osrm.org/route/v1/driving";

    private final RestClient restClient;

    public OsrmRoutesProvider() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<RouteResponse> getRoute(
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude) {

        try {
            String uri = String.format(
                    Locale.ROOT,
                    "%s/%f,%f;%f,%f?overview=full&geometries=polyline",
                    BASE_URL, originLongitude, originLatitude, destinationLongitude, destinationLatitude);

            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !"Ok".equals(response.get("code"))) {
                return Optional.empty();
            }

            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (routes == null || routes.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> route = routes.get(0);
            String geometry = (String) route.get("geometry");
            if (geometry == null) {
                return Optional.empty();
            }

            Number distance = (Number) route.get("distance");
            Number duration = (Number) route.get("duration");

            return Optional.of(RouteResponse.builder()
                    .distanceMeters(distance == null ? 0.0 : distance.doubleValue())
                    .durationSeconds(duration == null ? 0.0 : duration.doubleValue())
                    .encodedPolyline(geometry)
                    .provider("OSRM")
                    .build());

        } catch (Exception e) {
            log.warn("OSRM route call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
