package com.garageos.modules.navigation.service;

import com.garageos.core.enums.navigation.TripMediaShotType;
import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.modules.navigation.dto.response.NavigationTripMediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NavigationTripMediaService {

    NavigationTripMediaResponse upload(
            Long tripId,
            Long driverId,
            TripMediaStage stage,
            TripMediaShotType shotType,
            MultipartFile file,
            Double latitude,
            Double longitude
    );

    /**
     * Same as the 7-argument upload, plus a client-generated id for the captured file. Sending the same id again
     * (a retry after a dropped response, an app restart or a queue resume) returns the row already stored: no second
     * metadata row and no second cloud object.
     */
    NavigationTripMediaResponse upload(
            Long tripId,
            Long driverId,
            TripMediaStage stage,
            TripMediaShotType shotType,
            MultipartFile file,
            Double latitude,
            Double longitude,
            String clientUploadId
    );

    /**
     * Same as the 8-argument upload plus raw device capture metadata (capture time, GPS accuracy, resolved address).
     * The metadata is validated server-side; the capturing driver's identity always comes from the authenticated
     * principal, never from the request.
     */
    NavigationTripMediaResponse upload(
            Long tripId,
            Long driverId,
            TripMediaStage stage,
            TripMediaShotType shotType,
            MultipartFile file,
            Double latitude,
            Double longitude,
            String clientUploadId,
            CaptureMetadataSanitizer.Raw captureMetadata
    );

    List<NavigationTripMediaResponse> getTripMedia(
            Long tripId
    );

    List<NavigationTripMediaResponse> getTripMediaByStage(
            Long tripId,
            TripMediaStage stage
    );

    /**
     * Authorized read of one evidence photo's actual bytes — same viewer
     * rule as getTripMedia (the trip's own customer, its assigned driver,
     * or garage-matched operational staff).
     */
    TripMediaContent getMediaContent(Long tripId, Long mediaId);

    /** {@code variant} is "thumbnail" or anything else for the full evidence (the default). */
    TripMediaContent getMediaContent(Long tripId, Long mediaId, String variant);

    /**
     * Driver pickup (BEFORE_PICKUP) and delivery (DELIVERY) evidence of every trip linked to the job card, oldest first.
     * Only trips the caller may view (the same rule as a single trip) contribute; an unrelated caller gets an empty
     * list, never another booking's or garage's evidence. The bytes are served by the existing per-trip content
     * endpoint, so the same authorization applies there.
     */
    List<NavigationTripMediaResponse> getJobCardEvidence(Long jobCardId);
}
