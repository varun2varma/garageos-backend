package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripMediaShotType;
import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.dto.response.NavigationTripMediaResponse;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.entity.NavigationTripMedia;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import com.garageos.modules.navigation.security.NavigationTripAccessGuard;
import com.garageos.modules.navigation.service.NavigationTripMediaService;
import com.garageos.modules.navigation.service.TripMediaContent;
import com.garageos.modules.navigation.storage.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NavigationTripMediaServiceImpl
        implements NavigationTripMediaService {

    private final NavigationTripRepository
            navigationTripRepository;

    private final NavigationTripMediaRepository
            mediaRepository;

    private final MediaStorageService
            mediaStorageService;

    private final NavigationTripAccessGuard
            accessGuard;

    private final NavigationRequestRepository
            navigationRequestRepository;

    private final AuditService
            auditService;


    @Override
    @Transactional
    public NavigationTripMediaResponse upload(
            Long tripId,
            Long driverId,
            TripMediaStage stage,
            TripMediaShotType shotType,
            MultipartFile file,
            Double latitude,
            Double longitude) {

        // Root-cause fix: this previously trusted the client-supplied
        // driverId outright - any authenticated user of any role could
        // upload pickup/delivery evidence attributed to a different
        // driver's trip simply by putting that driver's id in the
        // request. Mirrors the identical corrective fix already applied
        // to every driver-side trip-transition endpoint in
        // NavigationTripServiceImpl.getDriverTrip/requireCallerIsDriver:
        // the caller's own identity is the authority, driverId is only
        // honoured when it matches the authenticated principal.
        requireCallerIsDriver(driverId);

        NavigationTrip trip =
                navigationTripRepository
                        .findByIdAndDriverId(
                                tripId,
                                driverId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Trip not found for driver"
                                )
                        );


        validateMediaUpload(
                trip,
                stage,
                file
        );


        String folder =
                "navigation/"
                        + tripId
                        + "/"
                        + stage.name().toLowerCase();


        String storageKey =
                mediaStorageService.upload(
                        file,
                        folder
                );


        NavigationTripMedia media =
                NavigationTripMedia.builder()

                        .tripId(tripId)

                        .mediaStage(stage)

                        .shotType(shotType)

                        .storageKey(storageKey)

                        .fileName(
                                file.getOriginalFilename()
                        )

                        .contentType(
                                file.getContentType()
                        )

                        .fileSize(
                                file.getSize()
                        )

                        .capturedBy(
                                driverId
                        )

                        .capturedAt(
                                LocalDateTime.now()
                        )

                        .latitude(latitude)

                        .longitude(longitude)

                        .build();


        NavigationTripMedia saved =
                mediaRepository.save(
                        media
                );

        auditService.record(
                AuditEventType.EVIDENCE_CAPTURED,
                "NavigationTripMedia",
                saved.getId(),
                resolveGarageId(tripId),
                java.util.Map.of("tripId", tripId, "stage", stage, "shotType", shotType == null ? "OTHER" : shotType),
                latitude,
                longitude
        );

        return toResponse(saved);
    }

    private Long resolveGarageId(Long tripId) {

        return navigationTripRepository.findById(tripId)
                .map(NavigationTrip::getNavigationRequestId)
                .flatMap(navigationRequestRepository::findById)
                .map(NavigationRequest::getGarageId)
                .orElse(null);
    }


    @Override
    @Transactional(readOnly = true)
    public List<NavigationTripMediaResponse>
    getTripMedia(Long tripId) {

        // Root-cause fix: this previously had no authorization at all -
        // any authenticated user could list any trip's pickup/delivery
        // evidence by guessing a tripId.
        accessGuard.authorizeViewer(currentPrincipal(), tripId);

        return mediaRepository
                .findByTripIdOrderByCapturedAtAsc(
                        tripId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<NavigationTripMediaResponse>
    getTripMediaByStage(
            Long tripId,
            TripMediaStage stage) {

        // Same fix as getTripMedia above.
        accessGuard.authorizeViewer(currentPrincipal(), tripId);

        return mediaRepository
                .findByTripIdAndMediaStageOrderByCapturedAtAsc(
                        tripId,
                        stage
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public TripMediaContent getMediaContent(Long tripId, Long mediaId) {

        accessGuard.authorizeViewer(currentPrincipal(), tripId);

        NavigationTripMedia media = mediaRepository.findById(mediaId)
                .filter(m -> m.getTripId() != null && m.getTripId().equals(tripId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Media not found for trip : " + tripId));

        byte[] content = mediaStorageService.readBytes(media.getStorageKey());

        String contentType = media.getContentType() != null ? media.getContentType() : "application/octet-stream";
        String fileName = media.getFileName() != null ? media.getFileName() : ("media-" + media.getId());

        return new TripMediaContent(content, contentType, fileName);
    }


    private void validateMediaUpload(
            NavigationTrip trip,
            TripMediaStage stage,
            MultipartFile file) {

        if (file == null
                || file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Media file is required"
            );
        }


        if (!isImage(file)) {

            throw new IllegalArgumentException(
                    "Only image files are allowed"
            );
        }


        if (trip.getStatus()
                != TripStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Media can only be uploaded while trip is in progress"
            );
        }


        if (stage
                == TripMediaStage.BEFORE_PICKUP) {

            if (trip.getTripType()
                    != TripType.PICKUP) {

                throw new IllegalStateException(
                        "BEFORE_PICKUP media is only valid for pickup trips"
                );
            }


            if (trip.getCurrentLeg()
                    != TripLeg.GARAGE_TO_CUSTOMER) {

                throw new IllegalStateException(
                        "BEFORE_PICKUP media must be captured during the pickup leg"
                );
            }


            if (trip.getArrivedAt() == null) {

                throw new IllegalStateException(
                        "Driver must arrive at customer before capturing pickup photos"
                );
            }
        }


        if (stage
                == TripMediaStage.DELIVERY) {

            if (trip.getTripType()
                    != TripType.DELIVERY) {

                throw new IllegalStateException(
                        "DELIVERY media is only valid for delivery trips"
                );
            }


            if (trip.getArrivedAt() == null) {

                throw new IllegalStateException(
                        "Driver must arrive at customer before capturing delivery photos"
                );
            }
        }
    }


    /**
     * A driver may only ever upload evidence as themselves. Deliberately
     * throws the same not-found-style error the trip lookup that follows
     * would throw, so passing another driver's id cannot be used to
     * distinguish "exists but not yours" from "does not exist" - mirrors
     * NavigationTripServiceImpl.requireCallerIsDriver.
     */
    private void requireCallerIsDriver(Long driverId) {

        GarageUserPrincipal principal = currentPrincipal();

        if (driverId == null
                || principal.getId() == null
                || !principal.getId().equals(driverId)) {

            throw new ResourceNotFoundException(
                    "Trip not found for driver."
            );
        }
    }

    private GarageUserPrincipal currentPrincipal() {

        return (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }


    private boolean isImage(
            MultipartFile file) {

        String contentType =
                file.getContentType();

        return contentType != null
                && contentType.startsWith(
                "image/"
        );
    }


    private NavigationTripMediaResponse
    toResponse(
            NavigationTripMedia media) {

        return NavigationTripMediaResponse
                .builder()

                .id(media.getId())

                .tripId(media.getTripId())

                .mediaStage(
                        media.getMediaStage()
                )

                .shotType(
                        media.getShotType()
                )

                .fileName(
                        media.getFileName()
                )

                .contentType(
                        media.getContentType()
                )

                .fileSize(
                        media.getFileSize()
                )

                .capturedBy(
                        media.getCapturedBy()
                )

                .capturedAt(
                        media.getCapturedAt()
                )

                .latitude(
                        media.getLatitude()
                )

                .longitude(
                        media.getLongitude()
                )

                .mediaUrl(
                        mediaStorageService.getUrl(
                                media.getStorageKey()
                        )
                )

                .build();
    }
}