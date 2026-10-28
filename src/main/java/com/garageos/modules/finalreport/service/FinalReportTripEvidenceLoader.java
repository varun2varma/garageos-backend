package com.garageos.modules.finalreport.service;

import com.garageos.core.enums.navigation.TripMediaStage;
import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO.TripEvidenceInfo;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.entity.NavigationTripMedia;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import com.garageos.modules.navigation.storage.MediaStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pickup and delivery evidence for the final report, selected by RELATIONSHIP and filtered by audience on the server.
 *
 * Which evidence belongs to the job card: only media of trips whose navigation request is linked to THIS job card
 * (navigation_request.job_card_id) and that request was made for this job card's garage and customer. Nothing is
 * matched by file name or storage path, so another job card's, customer's or garage's evidence cannot be pulled in
 * even if ids collide or a row is mislinked across garages.
 *
 * Audience: the OWNER report carries all of it. The CUSTOMER report carries only stages on an explicit allow-list
 * (the pickup and delivery condition photos the customer is already shown before confirming handover). Trip media has
 * no per-item visibility flag, so a stage not on the list is withheld from customers by default - a future internal
 * stage can never leak into a customer PDF. Videos are listed with their metadata but not embedded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinalReportTripEvidenceLoader {

    /** Stages a customer may see in their own report. */
    static final Set<TripMediaStage> CUSTOMER_VISIBLE_STAGES = EnumSet.of(TripMediaStage.BEFORE_PICKUP, TripMediaStage.DELIVERY);

    /** Bound on trip evidence items per report. */
    static final int MAX_ITEMS = 40;

    private final NavigationRequestRepository requestRepository;
    private final NavigationTripRepository tripRepository;
    private final NavigationTripMediaRepository mediaRepository;
    private final MediaStorageService storage;

    /**
     * Reads the photo from the key the row had when it was loaded. If that fails, the row may have been moved into its
     * job-card folder in the meantime (trip-evidence adoption), so the CURRENT key is read once from the database and
     * tried once. No lock and no wait: still unreadable means a genuine placeholder.
     */
    private byte[] readWithKeyRefresh(NavigationTripMedia m) {
        try {
            return storage.readBytes(m.getStorageKey());
        } catch (RuntimeException first) {
            String current = mediaRepository.findStorageKeyById(m.getId()).orElse(null);
            if (current != null && !current.equals(m.getStorageKey())) {
                return storage.readBytes(current);
            }
            throw first;
        }
    }

    public List<TripEvidenceInfo> load(JobCard jobCard, FinalReportAudience audience) {

        Long garageId = jobCard.getGarage() == null ? null : jobCard.getGarage().getId();
        Long customerId = jobCard.getCustomer() == null ? null : jobCard.getCustomer().getId();

        if (garageId == null || customerId == null) {
            return List.of();
        }

        Map<Long, NavigationRequest> requests = requestRepository.findByJobCardId(jobCard.getId()).stream()
                .filter(r -> garageId.equals(r.getGarageId()) && customerId.equals(r.getCustomerId()))
                .collect(Collectors.toMap(NavigationRequest::getId, r -> r, (a, b) -> a));

        if (requests.isEmpty()) {
            return List.of();
        }

        List<NavigationTripMedia> media = new ArrayList<>();
        for (NavigationTrip trip : tripRepository.findByNavigationRequestIdIn(requests.keySet())) {
            // defence in depth: the trip must really belong to one of the filtered requests
            if (!requests.containsKey(trip.getNavigationRequestId())) {
                continue;
            }
            for (NavigationTripMedia m : mediaRepository.findByTripIdOrderByCapturedAtAsc(trip.getId())) {
                // Same rule for both audiences: owner and customer reports share one evidence set. A trip
                // photo is included only when its persisted customer_visible flag is set AND its stage is
                // an allowed report stage - the stage alone never proves visibility.
                if (m.isCustomerVisible() && CUSTOMER_VISIBLE_STAGES.contains(m.getMediaStage())) {
                    media.add(m);
                }
            }
        }

        media.sort(Comparator.comparing(NavigationTripMedia::getCapturedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        List<TripEvidenceInfo> out = new ArrayList<>();

        for (NavigationTripMedia m : media) {

            if (out.size() >= MAX_ITEMS) {
                break;
            }

            boolean video = m.getContentType() != null && m.getContentType().toLowerCase().startsWith("video/");
            byte[] jpeg = null;

            if (!video) {
                try {
                    jpeg = ReportImageScaler.toReportJpeg(readWithKeyRefresh(m));
                } catch (RuntimeException e) {
                    // Missing / unreadable object: keep the metadata row, never fail the report.
                    log.warn("[FINAL-REPORT] Trip photo unavailable. jobCardId={}, mediaId={}, reason={}",
                            jobCard.getId(), m.getId(), e.getClass().getSimpleName());
                }
            }

            out.add(new TripEvidenceInfo(
                    m.getId(),
                    m.getMediaStage() == TripMediaStage.DELIVERY ? "Delivery" : "Pickup",
                    m.getShotType() == null ? null : m.getShotType().name(),
                    video,
                    m.getCapturedAt(),
                    m.getCapturedAtSource(),
                    m.getCreatedAt(),
                    m.getCapturedByName(),
                    m.getLatitude(),
                    m.getLongitude(),
                    m.getAccuracyMeters(),
                    m.getLocationAddress(),
                    jpeg));
        }

        return out;
    }
}
