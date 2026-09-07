package com.garageos.modules.navigation.dto.response;

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

    private String fileName;

    private String contentType;

    private Long fileSize;

    private Long capturedBy;

    private LocalDateTime capturedAt;

    private Double latitude;

    private Double longitude;

    private String mediaUrl;
}