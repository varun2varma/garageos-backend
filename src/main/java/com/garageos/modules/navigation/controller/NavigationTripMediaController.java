package com.garageos.modules.navigation.controller;

import com.garageos.core.enums.navigation.TripMediaShotType;
import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.modules.navigation.dto.response.NavigationTripMediaResponse;
import com.garageos.modules.navigation.service.NavigationTripMediaService;
import com.garageos.modules.navigation.service.TripMediaContent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

            @RequestParam(required = false)
            TripMediaShotType shotType,

            @RequestParam MultipartFile file,

            @RequestParam(required = false)
            Double latitude,

            @RequestParam(required = false)
            Double longitude) {

        return mediaService.upload(
                tripId,
                driverId,
                stage,
                shotType,
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


    /**
     * Serves the actual photo bytes — mirrors MediaController's job-card
     * content endpoint. Previously nothing served this at all: getUrl()
     * returned a "/media/..." path with no resource handler mapped to it.
     */
    @GetMapping("/{tripId}/media/{mediaId}/content")
    public ResponseEntity<byte[]> getMediaContent(
            @PathVariable Long tripId,
            @PathVariable Long mediaId) {

        TripMediaContent content = mediaService.getMediaContent(tripId, mediaId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.fileName() + "\"")
                .body(content.content());
    }
}