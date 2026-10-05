package com.garageos.modules.finalreport.service.impl;

import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.core.enums.report.FinalReportEmailStatus;
import com.garageos.core.integration.email.EmailDeliveryException;
import com.garageos.core.integration.email.EmailService;
import com.garageos.core.integration.email.EmailService.EmailAttachment;
import com.garageos.core.integration.email.EmailService.EmailMessage;
import com.garageos.core.util.InvoiceTaxCalculator;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.finalreport.entity.JobCardFinalReport;
import com.garageos.modules.finalreport.repository.JobCardFinalReportRepository;
import com.garageos.modules.finalreport.service.FinalReportEmailService;
import com.garageos.modules.garage.dto.response.GarageBrandingResponse;
import com.garageos.modules.garage.service.GarageBrandingService;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.invoice.service.InvoicePdfService;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.vehicle.entity.Vehicle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Service
public class FinalReportEmailServiceImpl implements FinalReportEmailService {

    static final int MAX_ATTEMPTS = 5;

    private static final EnumSet<FinalReportEmailStatus> CLAIMABLE = EnumSet.of(
            FinalReportEmailStatus.PENDING, FinalReportEmailStatus.FAILED, FinalReportEmailStatus.SENDING);

    private final JobCardFinalReportRepository reportRepository;
    private final JobCardRepository jobCardRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoicePdfService invoicePdfService;
    private final GarageBrandingService garageBrandingService;
    private final EmailService emailService;
    private final TransactionTemplate tx;
    private final long baseBackoffSeconds;
    private final long leaseMinutes;

    public FinalReportEmailServiceImpl(
            JobCardFinalReportRepository reportRepository,
            JobCardRepository jobCardRepository,
            InvoiceRepository invoiceRepository,
            InvoicePdfService invoicePdfService,
            GarageBrandingService garageBrandingService,
            EmailService emailService,
            TransactionTemplate tx,
            @Value("${final-report.email.base-backoff-seconds:120}") long baseBackoffSeconds,
            @Value("${final-report.email.lease-minutes:10}") long leaseMinutes) {
        this.reportRepository = reportRepository;
        this.jobCardRepository = jobCardRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoicePdfService = invoicePdfService;
        this.garageBrandingService = garageBrandingService;
        this.emailService = emailService;
        this.tx = tx;
        this.baseBackoffSeconds = baseBackoffSeconds;
        this.leaseMinutes = leaseMinutes;
    }

    /** Everything needed to send, read inside a transaction so lazy associations resolve. */
    record Prepared(String recipient, EmailMessage message) {
    }

    @Override
    public void dispatch(Long reportId) {

        if (!emailService.isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        Integer claimed = tx.execute(s -> reportRepository.claimForEmail(
                reportId, CLAIMABLE, now, now.plusMinutes(leaseMinutes)));

        if (claimed == null || claimed == 0) {
            return;
        }

        try {
            Prepared prepared = tx.execute(s -> prepare(reportId));

            if (prepared == null || prepared.recipient() == null || prepared.recipient().isBlank()) {
                record(reportId, FinalReportEmailStatus.SKIPPED, null, "No recipient e-mail address on record.", false);
                return;
            }

            emailService.send(prepared.message());

            record(reportId, FinalReportEmailStatus.SENT, prepared.recipient(), null, false);

            log.info("[FINAL-REPORT] E-mail sent. reportId={}", reportId);

        } catch (EmailDeliveryException e) {
            record(reportId, FinalReportEmailStatus.FAILED, null, e.getMessage(), true);
            log.warn("[FINAL-REPORT] E-mail failed, will retry. reportId={}", reportId);
        } catch (RuntimeException e) {
            // Never propagate: report e-mail problems must not reach the Job Card flow.
            record(reportId, FinalReportEmailStatus.FAILED, null,
                    "Unexpected error: " + e.getClass().getSimpleName(), true);
            log.error("[FINAL-REPORT] E-mail preparation failed. reportId={}", reportId, e);
        }
    }

    // ------------------------------------------------------------------ prepare

    private Prepared prepare(Long reportId) {

        JobCardFinalReport report = reportRepository.findById(reportId).orElseThrow();
        JobCard jobCard = jobCardRepository.findById(report.getJobCardId()).orElseThrow();
        GarageBrandingResponse garage = garageBrandingService.resolve(report.getGarageId());
        Vehicle vehicle = jobCard.getVehicle();
        Customer customer = jobCard.getCustomer();

        Invoice invoice = invoiceRepository.findByEstimateJobCardId(jobCard.getId()).orElse(null);
        BigDecimal total = null;
        String invoiceNumber = null;
        boolean paid = false;
        if (invoice != null) {
            InvoiceTaxCalculator.Breakdown b = invoicePdfService.breakdownFor(invoice);
            total = b.grandTotal();
            invoiceNumber = invoice.getInvoiceNumber();
            paid = invoice.getPaymentStatus() == PaymentStatus.PAID;
        }

        String vehicleText = (vehicle.getBrand() == null ? "" : vehicle.getBrand() + " ")
                + (vehicle.getModel() == null ? "" : vehicle.getModel() + " ")
                + "(" + vehicle.getRegistrationNumber() + ")";

        EmailAttachment attachment = new EmailAttachment(
                report.getFileName(), report.getContentType(), report.getContent());

        String garageName = garage.getGarageName() == null ? "your garage" : garage.getGarageName();
        String jcNumber = jobCard.getJobCardNumber();

        StringBuilder body = new StringBuilder();

        if (report.getAudience() == FinalReportAudience.CUSTOMER) {

            body.append("Hello ").append(customer.getFullName()).append(",\n\n")
                    .append("Your vehicle ").append(vehicleText.trim())
                    .append(" has been serviced and delivered by ").append(garageName).append(".\n\n")
                    .append("Job Card: ").append(jcNumber).append('\n');
            if (invoiceNumber != null) {
                body.append("Invoice: ").append(invoiceNumber).append('\n');
            }
            if (paid && total != null) {
                body.append("Amount paid: Rs. ").append(total.setScale(2, RoundingMode.HALF_UP).toPlainString()).append('\n');
            }
            body.append("Status: Service completed and vehicle delivered\n\n")
                    .append("Your service report is attached.\n\n")
                    .append("Regards,\n").append(garageName).append("\nPowered by GarageST\n");

            return new Prepared(customer.getEmail(), new EmailMessage(
                    customer.getEmail(),
                    "Your GarageST Vehicle Service Report – " + jcNumber,
                    body.toString(),
                    List.of(attachment)));
        }

        String recipient = garage.getEmail();

        body.append("Job Card ").append(jcNumber).append(" has been closed.\n\n")
                .append("Customer: ").append(customer.getFullName()).append('\n')
                .append("Vehicle: ").append(vehicleText.trim()).append('\n');
        if (invoiceNumber != null) {
            body.append("Invoice: ").append(invoiceNumber).append('\n');
        }
        if (paid && total != null) {
            body.append("Amount received: Rs. ").append(total.setScale(2, RoundingMode.HALF_UP).toPlainString()).append('\n');
        }
        body.append("\nThe owner report (internal) is attached. Do not forward it to the customer; "
                        + "the customer receives a separate report.\n\n")
                .append("Powered by GarageST\n");

        return new Prepared(recipient, new EmailMessage(
                recipient,
                "Job Card Closed – " + jcNumber,
                body.toString(),
                List.of(attachment)));
    }

    // ------------------------------------------------------------------- record

    private void record(Long reportId, FinalReportEmailStatus status, String recipient, String error, boolean failure) {

        tx.executeWithoutResult(s -> reportRepository.findById(reportId).ifPresent(r -> {
            LocalDateTime now = LocalDateTime.now();
            r.setEmailStatus(status);

            if (failure) {
                r.setEmailAttempts(r.getEmailAttempts() + 1);
                long delay = Math.min(6 * 3600L, baseBackoffSeconds * (1L << Math.min(r.getEmailAttempts(), 10)));
                r.setEmailNextAttemptAt(now.plusSeconds(delay));
            } else {
                r.setEmailNextAttemptAt(null);
            }

            if (status == FinalReportEmailStatus.SENT) {
                r.setEmailSentAt(now);
                r.setEmailRecipient(recipient);
                r.setEmailLastError(null);
            } else {
                r.setEmailLastError(error == null ? null : error.substring(0, Math.min(error.length(), 500)));
            }

            reportRepository.save(r);
        }));
    }
}
