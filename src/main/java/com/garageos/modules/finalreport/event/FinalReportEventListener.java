package com.garageos.modules.finalreport.event;

import com.garageos.modules.finalreport.service.FinalReportService;
import com.garageos.modules.jobcard.event.JobCardClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Generates the final reports after the Job Card closure has committed, on a
 * background thread. Whatever happens here - PDF failure, storage failure,
 * e-mail failure - the Job Card stays CLOSED; the report can be (re)generated
 * on demand by the owner/manager.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinalReportEventListener {

    private final FinalReportService finalReportService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobCardClosed(JobCardClosedEvent event) {
        try {
            finalReportService.generateOnClosure(event.jobCardId());
        } catch (RuntimeException e) {
            log.error("[FINAL-REPORT] Generation after closure failed. jobCardId={}", event.jobCardId(), e);
        }
    }
}
