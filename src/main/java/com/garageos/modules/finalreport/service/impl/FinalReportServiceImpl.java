package com.garageos.modules.finalreport.service.impl;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.core.enums.report.FinalReportEmailStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.delivery.repository.DeliveryRepository;
import com.garageos.modules.finalreport.dto.response.FinalReportStatusResponse;
import com.garageos.modules.finalreport.dto.response.JobCardFinalReportDTO;
import com.garageos.modules.finalreport.entity.JobCardFinalReport;
import com.garageos.modules.finalreport.pdf.FinalReportPdfRenderer;
import com.garageos.modules.finalreport.repository.JobCardFinalReportRepository;
import com.garageos.modules.finalreport.service.FinalReportAssembler;
import com.garageos.modules.finalreport.service.FinalReportEmailService;
import com.garageos.modules.finalreport.service.FinalReportService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinalReportServiceImpl implements FinalReportService {

    /** Membership-based tenant scope (field-injected so hand-built unit-test instances still work). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.core.security.GarageScope garageScope;

    private final JobCardRepository jobCardRepository;
    private final JobCardFinalReportRepository reportRepository;
    private final DeliveryRepository deliveryRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final FinalReportAssembler assembler;
    private final FinalReportPdfRenderer renderer;
    private final FinalReportEmailService emailService;
    private final TransactionTemplate tx;

    /** What a caller may do, decided from their relationship to the Job Card. */
    record Access(boolean canViewOwner, boolean canViewCustomer, boolean canRegenerate, boolean staff) {
    }

    // -------------------------------------------------------------- read paths

    @Override
    public FinalReportStatusResponse getStatus(GarageUserPrincipal principal, Long jobCardId) {

        return tx.execute(status -> {
            JobCard jobCard = jobCard(jobCardId);
            Access access = access(principal, jobCard);

            boolean ready = isReady(jobCard);
            List<FinalReportStatusResponse.Item> items = new ArrayList<>();

            if (ready) {
                for (FinalReportAudience audience : FinalReportAudience.values()) {
                    if (!allowed(access, audience)) {
                        continue;
                    }
                    reportRepository.findFirstByJobCardIdAndAudienceOrderByVersionDesc(
                                    jobCardId, audience, JobCardFinalReportRepository.ReportMeta.class)
                            .ifPresent(r -> items.add(FinalReportStatusResponse.Item.builder()
                                    .audience(r.getAudience())
                                    .version(r.getVersion())
                                    .fileName(r.getFileName())
                                    .fileSize(r.getFileSize())
                                    .generatedAt(r.getGeneratedAt())
                                    .withheld(r.getWithheld())
                                    .emailStatus(access.staff() ? r.getEmailStatus().name() : null)
                                    .build()));
                }
            }

            return FinalReportStatusResponse.builder()
                    .jobCardId(jobCardId)
                    .ready(ready)
                    .canViewCustomerReport(access.canViewCustomer())
                    .canViewOwnerReport(access.canViewOwner())
                    .canRegenerate(access.canRegenerate() && ready)
                    .reports(items)
                    .build();
        });
    }

    @Override
    public FinalReportFile download(
            GarageUserPrincipal principal, Long jobCardId, FinalReportAudience audience, Integer version) {

        return tx.execute(status -> {
            JobCard jobCard = jobCard(jobCardId);
            Access access = access(principal, jobCard);

            if (!allowed(access, audience)) {
                throw new AccessDeniedException("You are not allowed to view this report.");
            }

            Optional<JobCardFinalReport> report = version == null
                    ? reportRepository.findFirstByJobCardIdAndAudienceOrderByVersionDesc(jobCardId, audience)
                    : reportRepository.findByJobCardIdAndAudienceAndVersion(jobCardId, audience, version);

            JobCardFinalReport r = report.orElseThrow(() ->
                    new ResourceNotFoundException("The final report is not available yet."));

            // Privacy: a withheld version (legacy owner report, or media visibility withdrawn) is never
            // served - not as "latest" and not by explicit version. The default route never falls back
            // to an older version either, because the latest version is the only candidate.
            if (r.isWithheld()) {
                throw new BusinessException(
                        "This report version is withheld for privacy and cannot be downloaded. "
                                + "Ask the garage owner or manager to regenerate the report.",
                        "FINAL_REPORT_WITHHELD");
            }

            return new FinalReportFile(r.getContent(), r.getFileName(), r.getContentType(), r.getVersion());
        });
    }

    // ------------------------------------------------------------ write paths

    @Override
    public FinalReportStatusResponse regenerate(GarageUserPrincipal principal, Long jobCardId) {

        try {
            tx.executeWithoutResult(status -> {
                JobCard jobCard = jobCard(jobCardId);
                Access access = access(principal, jobCard);

                if (!access.canRegenerate()) {
                    throw new AccessDeniedException("Only the garage owner or manager can regenerate reports.");
                }

                requireReady(jobCard);

                // A regenerated version is available to download but is not e-mailed again.
                for (FinalReportAudience audience : FinalReportAudience.values()) {
                    issue(jobCard, audience, principal.getId(), FinalReportEmailStatus.SKIPPED);
                }
            });
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Report generation is already in progress. Please retry in a moment.");
        }

        return getStatus(principal, jobCardId);
    }

    @Override
    public void generateOnClosure(Long jobCardId) {

        List<Long> issued;

        try {
            issued = tx.execute(status -> {
                JobCard jobCard = jobCard(jobCardId);

                if (!isReady(jobCard)) {
                    log.warn("[FINAL-REPORT] Not generated: Job Card not complete. jobCardId={}", jobCardId);
                    return List.<Long>of();
                }

                List<Long> ids = new ArrayList<>();
                for (FinalReportAudience audience : FinalReportAudience.values()) {
                    if (!reportRepository.existsByJobCardIdAndAudience(jobCardId, audience)) {
                        ids.add(issue(jobCard, audience, null, FinalReportEmailStatus.PENDING).getId());
                    }
                }
                return ids;
            });
        } catch (DataIntegrityViolationException e) {
            // Another generator issued the same version first; nothing to do.
            log.info("[FINAL-REPORT] Already generated concurrently. jobCardId={}", jobCardId);
            return;
        }

        // After the commit, outside any transaction: an e-mail problem is recorded on the
        // report for retry and can never affect the Job Card or the stored document.
        for (Long id : issued) {
            emailService.dispatch(id);
        }
    }

    // ----------------------------------------------------------------- helpers

    private JobCardFinalReport issue(
            JobCard jobCard, FinalReportAudience audience, Long generatedBy, FinalReportEmailStatus emailStatus) {

        int version = reportRepository.findFirstByJobCardIdAndAudienceOrderByVersionDesc(jobCard.getId(), audience)
                .map(r -> r.getVersion() + 1)
                .orElse(1);

        LocalDateTime now = LocalDateTime.now();

        JobCardFinalReportDTO dto = assembler.assemble(jobCard, audience, version, now);
        byte[] pdf = renderer.render(dto);

        JobCardFinalReport entity = new JobCardFinalReport();
        entity.setJobCardId(jobCard.getId());
        entity.setGarageId(jobCard.getGarage().getId());
        entity.setAudience(audience);
        entity.setVersion(version);
        entity.setFileName(fileName(jobCard, audience, version));
        entity.setFileSize(pdf.length);
        entity.setContent(pdf);
        entity.setGeneratedBy(generatedBy);
        entity.setGeneratedAt(now);
        entity.setEmailStatus(emailStatus);

        return reportRepository.saveAndFlush(entity);
    }

    static String fileName(JobCard jobCard, FinalReportAudience audience, int version) {
        String safe = jobCard.getJobCardNumber().replaceAll("[^A-Za-z0-9._-]", "_");
        return "GarageST_" + safe + "_"
                + (audience == FinalReportAudience.OWNER ? "Owner" : "Customer")
                + "_Report" + (version > 1 ? "_v" + version : "") + ".pdf";
    }

    private JobCard jobCard(Long id) {
        return jobCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job Card not found."));
    }

    /** Paid + delivered + CLOSED. Anything less never produces a final report. */
    boolean isReady(JobCard jobCard) {

        if (jobCard.getStatus() != JobCardStatus.CLOSED) {
            return false;
        }

        if (!deliveryRepository.existsByJobCardId(jobCard.getId())) {
            return false;
        }

        Optional<Invoice> invoice = invoiceRepository.findByEstimateJobCardId(jobCard.getId());

        return invoice.isPresent() && invoice.get().getPaymentStatus() == PaymentStatus.PAID;
    }

    private void requireReady(JobCard jobCard) {
        if (!isReady(jobCard)) {
            throw new BusinessException(
                    "The final report is available only after payment, delivery and Job Card closure.",
                    "FINAL_REPORT_NOT_READY");
        }
    }

    private static boolean allowed(Access access, FinalReportAudience audience) {
        return audience == FinalReportAudience.OWNER ? access.canViewOwner() : access.canViewCustomer();
    }

    /**
     * Staff of the Job Card's garage: OWNER/MANAGER see both reports and may
     * regenerate; SERVICE_ADVISOR sees the customer report; everyone else on the
     * garage (e.g. TECHNICIAN) sees neither. The customer who owns the Job Card
     * sees the customer report only. Anyone else gets not-found.
     */
    Access access(GarageUserPrincipal principal, JobCard jobCard) {

        if (jobCard.getGarage() != null
                && com.garageos.core.security.GarageScope.isMemberOfGarage(garageScope, principal, jobCard.getGarage().getId())) {

            Set<String> roles = principal.getRoles() == null ? Set.of() : principal.getRoles();
            boolean ownerLevel = roles.contains(RoleCode.OWNER.name()) || roles.contains(RoleCode.MANAGER.name());
            boolean advisor = roles.contains(RoleCode.SERVICE_ADVISOR.name());

            return new Access(ownerLevel, ownerLevel || advisor, ownerLevel, true);
        }

        if (principal.getMobile() != null) {
            Customer customer = customerRepository.findByMobileNumber(principal.getMobile()).orElse(null);

            if (customer != null
                    && jobCard.getCustomer() != null
                    && jobCard.getCustomer().getId().equals(customer.getId())) {
                return new Access(false, true, false, false);
            }
        }

        throw new ResourceNotFoundException("Job Card not found.");
    }
}
