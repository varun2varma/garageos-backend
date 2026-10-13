package com.garageos.modules.navigation.dto.response;

import com.garageos.core.enums.navigation.TripMediaShotType;
import com.garageos.core.enums.navigation.TripMediaStage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NavigationTripMediaResponse {

    private Long id;

    private Long tripId;

    private TripMediaStage mediaStage;

    private TripMediaShotType shotType;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private Long capturedBy;

    private LocalDateTime capturedAt;

    private Double latitude;

    private Double longitude;

    /** Metadata below is null for evidence captured before V74 ("not recorded"). */
    private String capturedByName;

    private String capturedAtSource;

    private Double accuracyMeters;

    private String locationAddress;

    /** Server-recorded acceptance time; distinct from capturedAt for offline uploads. */
    private LocalDateTime uploadedAt;

    private String mediaUrl;
}