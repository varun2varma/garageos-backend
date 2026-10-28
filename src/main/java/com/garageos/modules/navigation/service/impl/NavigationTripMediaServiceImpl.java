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
import com.garageos.modules.media.util.ImageOrientation;
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

    /** Optional: when absent (or when it cannot resolve a garage code) the previous key layout is used. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.modules.navigation.storage.TripEvidenceFolderResolver folderResolver;


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

        return upload(tripId, driverId, stage, shotType, file, latitude, longitude, null);
    }

    @Override
    @Transactional
    public NavigationTripMediaResponse upload(
            Long tripId,
            Long driverId,
            TripMediaStage stage,
            TripMediaShotType shotType,
            MultipartFile file,
            Double latitude,
            Double longitude,
            String clientUploadId) {

        return upload(tripId, driverId, stage, shotType, file, latitude, longitude, clientUploadId, null);
    }

    @Override
    @Transactional
    public NavigationTripMediaResponse upload(
            Long tripId,
            Long driverId,
            TripMediaStage stage,
            TripMediaShotType shotType,
            MultipartFile file,
            Double latitude,
            Double longitude,
            String clientUploadId,
            com.garageos.modules.navigation.service.CaptureMetadataSanitizer.Raw captureMetadata) {

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


        String uploadId = clientUploadId == null || clientUploadId.isBlank() ? null : clientUploadId.trim();

        if (uploadId != null && uploadId.length() > 80) {
            throw new IllegalArgumentException("clientUploadId is at most 80 characters");
        }

        if (uploadId != null) {
            // Retry of a file that was already stored: return it, create nothing.
            java.util.Optional<NavigationTripMedia> existing =
                    mediaRepository.findByTripIdAndClientUploadId(tripId, uploadId);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        validateMediaUpload(
                trip,
                stage,
                file
        );


        // Tenant-scoped object key: garage / trip / stage / unique id (the id is added by the storage layer), e.g.
        // trip-evidence/garage-12/navigation/55/before_pickup/<uuid>.jpg. Objects written before this convention keep
        // their stored key (the key is read from the row, never rebuilt), so nothing is renamed or lost. If the
        // garage cannot be resolved the previous layout is used rather than failing the upload.
        Long evidenceGarageId = trip.getNavigationRequestId() == null ? null
                : navigationRequestRepository.findById(trip.getNavigationRequestId())
                        .map(NavigationRequest::getGarageId).orElse(null);

        String legacyFolder =
                (evidenceGarageId == null ? "" : "garage-" + evidenceGarageId + "/")
                        + "navigation/"
                        + tripId
                        + "/"
                        + stage.name().toLowerCase();

        // New layout (server-side records only): GarageST/<garageCode>/<jobCardNumber|TRIP-<tripId>>/DURING_PICKUP|DURING_DELIVERY
        String folder = folderResolver == null
                ? legacyFolder
                : folderResolver.folderFor(trip, stage).orElse(legacyFolder);


        // Photos: bake the EXIF orientation into the pixels once (the stored bytes then carry no orientation tag, so
        // no consumer can apply it a second time). Untagged/normal images and videos are stored byte-for-byte.
        byte[] stored = null;
        String storedContentType = file.getContentType();
        long storedSize = file.getSize();

        if ("image/jpeg".equalsIgnoreCase(file.getContentType()) || "image/jpg".equalsIgnoreCase(file.getContentType())) {
            try {
                byte[] original = file.getBytes();
                if (ImageOrientation.readJpegOrientation(original) != ImageOrientation.NORMAL) {
                    java.awt.image.BufferedImage upright = ImageOrientation.decodeUpright(original);
                    if (upright != null) {
                        stored = ImageOrientation.encodeJpeg(upright, 0.92f);
                        storedContentType = "image/jpeg";
                        storedSize = stored.length;
                    }
                }
            } catch (java.io.IOException | RuntimeException ex) {
                // Undecodable: keep the original bytes rather than lose the evidence.
                stored = null;
            }
        }

        GarageUserPrincipal uploader = currentPrincipal();
        String uploaderName = ((uploader.getFirstName() == null ? "" : uploader.getFirstName()) + " "
                + (uploader.getLastName() == null ? "" : uploader.getLastName())).trim();

        // Device-supplied metadata is validated; coordinates may also arrive via the legacy latitude/longitude params.
        com.garageos.modules.navigation.service.CaptureMetadataSanitizer.Raw rawMeta =
                captureMetadata == null
                        ? new com.garageos.modules.navigation.service.CaptureMetadataSanitizer.Raw(null, latitude, longitude, null, null)
                        : new com.garageos.modules.navigation.service.CaptureMetadataSanitizer.Raw(
                                captureMetadata.capturedAt(),
                                captureMetadata.latitude() != null ? captureMetadata.latitude() : latitude,
                                captureMetadata.longitude() != null ? captureMetadata.longitude() : longitude,
                                captureMetadata.accuracyMeters(),
                                captureMetadata.locationAddress());
        com.garageos.modules.navigation.service.CaptureMetadataSanitizer.Result meta =
                com.garageos.modules.navigation.service.CaptureMetadataSanitizer.sanitize(
                        rawMeta, java.time.Instant.now(), java.time.ZoneId.systemDefault());

        // Canonical path: an image, R2 available and the job card already linked (delivery, or pickup after the link).
        // The upright evidence and a genuine thumbnail are written under the job-card naming convention from the start.
        // Everything else (videos, no job card yet = provisional pickup, no R2, undecodable image) keeps the previous path.
        com.garageos.modules.navigation.storage.TripEvidenceFolderResolver.Placement placement =
                folderResolver == null ? null : folderResolver.placementFor(trip, stage).orElse(null);
        com.garageos.modules.navigation.storage.TripEvidenceImages.Rendition rendition = null;

        if (placement != null && placement.jobCardNumber() != null && mediaStorageService.supportsObjectKeys()
                && file.getContentType() != null && file.getContentType().toLowerCase().startsWith("image/")) {
            try {
                rendition = com.garageos.modules.navigation.storage.TripEvidenceImages.render(file.getBytes());
            } catch (java.io.IOException | RuntimeException ex) {
                rendition = null;
            }
        }

        if (rendition != null) {
            return saveCanonical(tripId, driverId, stage, shotType, file, uploadId, meta, uploaderName, placement, rendition);
        }

        String storageKey = stored != null
                ? mediaStorageService.storeEvidence(stored, storedContentType, folder, ".jpg")
                : mediaStorageService.upload(file, folder);

        NavigationTripMedia media =
                NavigationTripMedia.builder()

                        .tripId(tripId)

                        .mediaStage(stage)

                        .customerVisible(stage != null && stage.defaultCustomerVisible())

                        .shotType(shotType)

                        .storageKey(storageKey)

                        .fileName(
                                file.getOriginalFilename()
                        )

                        .clientUploadId(uploadId)

                        .contentType(
                                storedContentType
                        )

                        .fileSize(
                                storedSize
                        )

                        .capturedBy(
                                driverId
                        )

                        .capturedAt(meta.capturedAt())

                        .capturedAtSource(meta.capturedAtSource())

                        .capturedByName(uploaderName.isEmpty() ? null : uploaderName)

                        .latitude(meta.latitude())

                        .longitude(meta.longitude())

                        .accuracyMeters(meta.accuracyMeters())

                        .locationAddress(meta.locationAddress())

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
                meta.latitude(),
                meta.longitude()
        );

        return toResponse(saved);
    }

    /**
     * Stores one photo under its canonical keys. Uploads of one job card and stage are serialised by a transaction-scoped
     * advisory lock; the row is inserted first (its id fixes the sequence: 1 + the number of lower-id photos of that
     * job card and stage), then evidence and thumbnail are written, then the row gets the evidence key. Any failure rolls
     * the whole transaction back, and a client retry (same upload id) starts again and lands on the same keys.
     */
    private NavigationTripMediaResponse saveCanonical(
            Long tripId, Long driverId, TripMediaStage stage, TripMediaShotType shotType,
            MultipartFile file, String uploadId,
            com.garageos.modules.navigation.service.CaptureMetadataSanitizer.Result meta, String uploaderName,
            com.garageos.modules.navigation.storage.TripEvidenceFolderResolver.Placement placement,
            com.garageos.modules.navigation.storage.TripEvidenceImages.Rendition rendition) {

        Long jobCardId = placement.jobCardId();

        mediaRepository.lockStageSequence((int) (jobCardId % Integer.MAX_VALUE), 0x54524950 + stage.ordinal());

        NavigationTripMedia pending = mediaRepository.saveAndFlush(
                NavigationTripMedia.builder()
                        .tripId(tripId)
                        .mediaStage(stage)
                        .customerVisible(stage != null && stage.defaultCustomerVisible())
                        .shotType(shotType)
                        .storageKey("pending/" + java.util.UUID.randomUUID()) // replaced below, inside this transaction
                        .fileName(file.getOriginalFilename())
                        .clientUploadId(uploadId)
                        .contentType("image/jpeg")
                        .fileSize((long) rendition.evidence().length)
                        .capturedBy(driverId)
                        .capturedAt(meta.capturedAt())
                        .capturedAtSource(meta.capturedAtSource())
                        .capturedByName(uploaderName.isEmpty() ? null : uploaderName)
                        .latitude(meta.latitude())
                        .longitude(meta.longitude())
                        .accuracyMeters(meta.accuracyMeters())
                        .locationAddress(meta.locationAddress())
                        .build());

        int sequence = (int) mediaRepository.countStageRowsBefore(jobCardId, stage, pending.getId()) + 1;

        String evidenceKey = com.garageos.modules.navigation.storage.TripEvidenceKeys.canonicalEvidenceKey(
                placement.garageCode(), placement.jobCardNumber(), stage, sequence);

        if (evidenceKey == null) {
            throw new IllegalStateException("Could not build the evidence key for this photo.");
        }

        String thumbnailKey = com.garageos.modules.navigation.storage.TripEvidenceKeys.thumbnailKeyOf(evidenceKey);

        mediaStorageService.putObject(evidenceKey, rendition.evidence(), "image/jpeg");
        mediaStorageService.putObject(thumbnailKey, rendition.thumbnail(), "image/jpeg");

        pending.setStorageKey(evidenceKey);

        NavigationTripMedia saved = mediaRepository.save(pending);

        recordCaptured(saved, tripId, stage, shotType, meta);

        return toResponse(saved);
    }

    private void recordCaptured(NavigationTripMedia saved, Long tripId, TripMediaStage stage, TripMediaShotType shotType,
                                com.garageos.modules.navigation.service.CaptureMetadataSanitizer.Result meta) {
        auditService.record(
                AuditEventType.EVIDENCE_CAPTURED,
                "NavigationTripMedia",
                saved.getId(),
                resolveGarageId(tripId),
                java.util.Map.of("tripId", tripId, "stage", stage, "shotType", shotType == null ? "OTHER" : shotType),
                meta.latitude(),
                meta.longitude()
        );
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
    public List<NavigationTripMediaResponse> getJobCardEvidence(Long jobCardId) {

        GarageUserPrincipal principal = currentPrincipal();

        List<NavigationRequest> requests = navigationRequestRepository.findByJobCardId(jobCardId);

        if (requests.isEmpty()) {
            return List.of();
        }

        java.util.Map<Long, NavigationRequest> byId = new java.util.HashMap<>();
        requests.forEach(r -> byId.put(r.getId(), r));

        List<NavigationTrip> trips = navigationTripRepository.findByNavigationRequestIdIn(byId.keySet());

        List<NavigationTripMediaResponse> out = new java.util.ArrayList<>();

        for (NavigationTrip trip : trips) {
            try {
                accessGuard.authorizeViewer(principal, trip, byId.get(trip.getNavigationRequestId()));
            } catch (ResourceNotFoundException ex) {
                continue; // not this caller's trip: contributes nothing
            }
            mediaRepository.findByTripIdOrderByCapturedAtAsc(trip.getId())
                    .forEach(m -> out.add(toResponse(m)));
        }

        out.sort(java.util.Comparator.comparing(NavigationTripMediaResponse::getCapturedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public TripMediaContent getMediaContent(Long tripId, Long mediaId) {
        return getMediaContent(tripId, mediaId, null);
    }

    /**
     * Evidence by default; {@code variant=thumbnail} serves the thumbnail of a canonical photo. Same authorization, trip
     * and media lookup as the full image. A photo without a thumbnail (legacy / provisional keys, videos, or an object that
     * is not there) is answered as not found (the unavailable response), never with a URL or bytes of something else.
     */
    @Override
    @Transactional(readOnly = true)
    public TripMediaContent getMediaContent(Long tripId, Long mediaId, String variant) {

        accessGuard.authorizeViewer(currentPrincipal(), tripId);

        NavigationTripMedia media = mediaRepository.findById(mediaId)
                .filter(m -> m.getTripId() != null && m.getTripId().equals(tripId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Media not found for trip : " + tripId));

        if ("thumbnail".equalsIgnoreCase(variant)) {

            String evidenceKey = media.getStorageKey();

            if (!com.garageos.modules.navigation.storage.TripEvidenceKeys.isCanonicalEvidenceKey(evidenceKey)
                    || !mediaStorageService.supportsObjectKeys()) {
                throw new ResourceNotFoundException("Thumbnail not available for media : " + mediaId);
            }

            String thumbnailKey = com.garageos.modules.navigation.storage.TripEvidenceKeys.thumbnailKeyOf(evidenceKey);

            if (!mediaStorageService.exists(thumbnailKey)) {
                throw new ResourceNotFoundException("Thumbnail not available for media : " + mediaId);
            }

            return new TripMediaContent(mediaStorageService.readBytes(thumbnailKey), "image/jpeg",
                    "thumbnail-" + media.getId() + ".jpg");
        }

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


        if (!isImageOrVideo(file)) {

            throw new IllegalArgumentException(
                    "Only image or video files are allowed"
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


    private boolean isImageOrVideo(
            MultipartFile file) {

        String contentType =
                file.getContentType();

        return contentType != null
                && (contentType.startsWith("image/")
                || contentType.startsWith("video/"));
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

                .capturedByName(media.getCapturedByName())

                .capturedAtSource(media.getCapturedAtSource())

                .accuracyMeters(media.getAccuracyMeters())

                .locationAddress(media.getLocationAddress())

                .uploadedAt(media.getCreatedAt())

                .mediaUrl(
                        mediaStorageService.getUrl(
                                media.getStorageKey()
                        )
                )

                .build();
    }
}