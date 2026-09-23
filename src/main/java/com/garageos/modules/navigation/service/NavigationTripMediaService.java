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
}