package com.garageos.modules.navigation.controller;

import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.modules.navigation.dto.response.NavigationTripMediaResponse;
import com.garageos.modules.navigation.service.NavigationTripMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/navigation/trips")
@RequiredArgsConstructor
public class NavigationTripMediaController {

    private final NavigationTripMediaService
            mediaService;


    @PostMapping(
            value = "/{tripId}/media",
            consumes = "multipart/form-data"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public NavigationTripMediaResponse upload(
            @PathVariable Long tripId,

            @RequestParam Long driverId,

            @RequestParam TripMediaStage stage,

            @RequestParam MultipartFile file,

            @RequestParam(required = false)
            Double latitude,

            @RequestParam(required = false)
            Double longitude) {

        return mediaService.upload(
                tripId,
                driverId,
                stage,
                file,
                latitude,
                longitude
        );
    }


    @GetMapping("/{tripId}/media")
    public List<NavigationTripMediaResponse>
    getTripMedia(
            @PathVariable Long tripId) {

        return mediaService.getTripMedia(
                tripId
        );
    }


    @GetMapping("/{tripId}/media/{stage}")
    public List<NavigationTripMediaResponse>
    getTripMediaByStage(
            @PathVariable Long tripId,
            @PathVariable TripMediaStage stage) {

        return mediaService
                .getTripMediaByStage(
                        tripId,
                        stage
                );
    }
}