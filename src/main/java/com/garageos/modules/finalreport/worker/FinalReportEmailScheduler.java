package com.garageos.modules.finalreport.worker;

import com.garageos.core.enums.report.FinalReportEmailStatus;
import com.garageos.core.integration.email.EmailService;
import com.garageos.modules.finalreport.repository.JobCardFinalReportRepository;
import com.garageos.modules.finalreport.service.FinalReportEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Retries report e-mails that are waiting (e.g. e-mail was switched off when
 * the report was issued), failed, or were claimed by a sender that died. Only
 * reports issued within {@code final-report.email.max-age-days} are picked up,
 * so turning e-mail on later never mails out old reports.
 */
@Slf4j
@Component
public class FinalReportEmailScheduler {

    private static final EnumSet<FinalReportEmailStatus> DUE = EnumSet.of(
            FinalReportEmailStatus.PENDING, FinalReportEmailStatus.FAILED, FinalReportEmailStatus.SENDING);

    private final JobCardFinalReportRepository reportRepository;
    private final FinalReportEmailService emailService;
    private final EmailService mailTransport;
    private final long maxAgeDays;
    private final int maxAttempts;

    public FinalReportEmailScheduler(
            JobCardFinalReportRepository reportRepository,
            FinalReportEmailService emailService,
            EmailService mailTransport,
            @Value("${final-report.email.max-age-days:7}") long maxAgeDays,
            @Value("${final-report.email.max-attempts:5}") int maxAttempts) {
        this.reportRepository = reportRepository;
        this.emailService = emailService;
        this.mailTransport = mailTransport;
        this.maxAgeDays = maxAgeDays;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${final-report.email.retry-delay-ms:60000}",
            initialDelayString = "${final-report.email.retry-delay-ms:60000}")
    public void retryDue() {

        if (!mailTransport.isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<Long> due = reportRepository.findDueForEmail(DUE, maxAttempts, now.minusDays(maxAgeDays), now);

        for (Long id : due) {
            emailService.dispatch(id);
        }
    }
}
