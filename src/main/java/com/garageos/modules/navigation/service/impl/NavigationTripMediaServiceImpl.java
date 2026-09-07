package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.modules.navigation.dto.response.NavigationTripMediaResponse;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.entity.NavigationTripMedia;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import com.garageos.modules.navigation.service.NavigationTripMediaService;
import com.garageos.modules.navigation.storage.MediaStorageService;
import lombok.RequiredArgsConstructor;
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


    @Override
    @Transactional
    public NavigationTripMediaResponse upload(
            Long tripId,
            Long driverId,
            TripMediaStage stage,
            MultipartFile file,
            Double latitude,
            Double longitude) {

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


        return toResponse(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public List<NavigationTripMediaResponse>
    getTripMedia(Long tripId) {

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

        return mediaRepository
                .findByTripIdAndMediaStageOrderByCapturedAtAsc(
                        tripId,
                        stage
                )
                .stream()
                .map(this::toResponse)
                .toList();
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