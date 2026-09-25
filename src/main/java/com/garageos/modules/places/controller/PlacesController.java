package com.garageos.modules.places.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.places.dto.response.PlaceSearchResult;
import com.garageos.modules.places.service.PlacesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Google-first/Nominatim-fallback place search for the Location Picker.
 * No @PreAuthorize: a place search carries no tenant/ownership data of its
 * own (SecurityConfig's default .anyRequest().authenticated() already
 * applies), matching NavigationRouteController's access level for the
 * equivalent Routes endpoint.
 */
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlacesController {

    private final PlacesService placesService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PlaceSearchResult>>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng) {

        return ApiResponseUtil.success(
                "Places fetched successfully.",
                placesService.search(query, lat, lng));
    }

    /**
     * Resolves a Google-suggestion placeId to coordinates - the client
     * calls this only once, for the suggestion the user actually taps, not
     * per keystroke (cost control - see GooglePlacesProvider doc comment).
     */
    @GetMapping("/details")
    public ResponseEntity<?> details(@RequestParam("placeId") String placeId) {

        return placesService.details(placeId)
                .<ResponseEntity<?>>map(place ->
                        ApiResponseUtil.success("Place details fetched successfully.", place))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
