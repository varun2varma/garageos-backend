package com.garageos.modules.navigation.service.impl;

import com.garageos.modules.navigation.dto.request.LatLngRequest;
import com.garageos.modules.navigation.dto.request.RouteRequest;
import com.garageos.modules.navigation.dto.response.RouteResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the Google-first/OSRM-fallback orchestration (mission Part Q/R):
 * Google is tried only when configured, OSRM is the fallback on any
 * Google failure or when Google isn't configured at all, and the two are
 * never called in parallel - each scenario asserts the provider NOT
 * expected to answer was never even called, not just that the right
 * result came back.
 */
@ExtendWith(MockitoExtension.class)
class NavigationRouteServiceImplTest {

    @Mock private GoogleRoutesProvider googleRoutesProvider;
    @Mock private OsrmRoutesProvider osrmRoutesProvider;

    private NavigationRouteServiceImpl service;

    private RouteRequest request() {
        RouteRequest request = new RouteRequest();
        LatLngRequest origin = new LatLngRequest();
        origin.setLatitude(17.385);
        origin.setLongitude(78.4867);
        LatLngRequest destination = new LatLngRequest();
        destination.setLatitude(17.4);
        destination.setLongitude(78.5);
        request.setOrigin(origin);
        request.setDestination(destination);
        return request;
    }

    @Test
    void googleConfiguredAndSucceeds_returnsGoogleResult_neverCallsOsrm() {
        service = new NavigationRouteServiceImpl(googleRoutesProvider, osrmRoutesProvider);
        when(googleRoutesProvider.isConfigured()).thenReturn(true);
        RouteResponse googleRoute = RouteResponse.builder().provider("GOOGLE").distanceMeters(1000.0).build();
        when(googleRoutesProvider.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(googleRoute));

        Optional<RouteResponse> result = service.getRoute(request());

        assertThat(result).contains(googleRoute);
        verify(osrmRoutesProvider, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void googleConfiguredButFails_fallsBackToOsrm() {
        service = new NavigationRouteServiceImpl(googleRoutesProvider, osrmRoutesProvider);
        when(googleRoutesProvider.isConfigured()).thenReturn(true);
        when(googleRoutesProvider.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        RouteResponse osrmRoute = RouteResponse.builder().provider("OSRM").distanceMeters(1200.0).build();
        when(osrmRoutesProvider.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(osrmRoute));

        Optional<RouteResponse> result = service.getRoute(request());

        assertThat(result).contains(osrmRoute);
    }

    @Test
    void googleNotConfigured_goesStraightToOsrm_neverCallsGoogleRoute() {
        service = new NavigationRouteServiceImpl(googleRoutesProvider, osrmRoutesProvider);
        when(googleRoutesProvider.isConfigured()).thenReturn(false);
        RouteResponse osrmRoute = RouteResponse.builder().provider("OSRM").distanceMeters(1200.0).build();
        when(osrmRoutesProvider.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(osrmRoute));

        Optional<RouteResponse> result = service.getRoute(request());

        assertThat(result).contains(osrmRoute);
        verify(googleRoutesProvider, never()).getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void bothProvidersFail_returnsEmpty_notAFabricatedRoute() {
        service = new NavigationRouteServiceImpl(googleRoutesProvider, osrmRoutesProvider);
        when(googleRoutesProvider.isConfigured()).thenReturn(true);
        when(googleRoutesProvider.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        when(osrmRoutesProvider.getRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());

        Optional<RouteResponse> result = service.getRoute(request());

        assertThat(result).isEmpty();
    }
}
