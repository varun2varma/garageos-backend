package com.garageos.modules.navigation.event;

import com.garageos.modules.navigation.service.TripEvidenceAdoptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs the pickup-evidence adoption after the job-card creation has committed, on a background thread (the same
 * pattern as the final-report listener). Whatever happens here, the job card stays created.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripEvidenceAdoptionListener {

    private final TripEvidenceAdoptionService adoptionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdoptionRequested(TripEvidenceAdoptionRequested event) {
        try {
            adoptionService.adoptForJobCard(event.jobCardId());
        } catch (RuntimeException e) {
            log.error("[TRIP-EVIDENCE] Adoption after job-card creation failed. jobCardId={}", event.jobCardId(), e);
        }
    }
}
