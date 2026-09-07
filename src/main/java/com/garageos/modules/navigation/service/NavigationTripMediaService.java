package com.garageos.modules.navigation.service;

import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.modules.navigation.dto.response.NavigationTripMediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface NavigationTripMediaService {

    NavigationTripMediaResponse upload(
            Long tripId,
            Long driverId,
            TripMediaStage stage,
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
}