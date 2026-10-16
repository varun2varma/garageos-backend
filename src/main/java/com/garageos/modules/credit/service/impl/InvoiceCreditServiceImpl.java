package com.garageos.modules.credit.service.impl;

import com.garageos.core.enums.CreditAuthorizationStatus;
import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.audit.AuditEventType;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.audit.service.AuditService;
import com.garageos.modules.credit.dto.AuthorizeCreditRequest;
import com.garageos.modules.credit.dto.CreditAuthorizationResponse;
import com.garageos.modules.credit.dto.RevokeCreditRequest;
import com.garageos.modules.credit.entity.InvoiceCreditAuthorization;
import com.garageos.modules.credit.repository.InvoiceCreditAuthorizationRepository;
import com.garageos.modules.credit.service.InvoiceCreditService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Owner credit authorization. Rules enforced here, server-side (the controller's role annotation is a
 * second layer, never the only one):
 * <ul>
 *   <li>only an OWNER of the invoice's own garage may authorize or revoke - a Manager never can;</li>
 *   <li>the invoice is locked first (same lock as payments), so concurrent authorize / revoke / pay calls
 *       serialise; a partial unique index additionally allows a single ACTIVE row per invoice;</li>
 *   <li>amount &gt; 0 and &lt;= the outstanding balance computed from the payment ledger; a due date after
 *       today is mandatory; the invoice must be issued, unpaid and not cancelled;</li>
 *   <li>authorizing never touches the invoice's payment status.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InvoiceCreditServiceImpl implements InvoiceCreditService {

    private static final String ENTITY = "InvoiceCreditAuthorization";

    private final InvoiceCreditAuthorizationRepository creditRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final JobCardService jobCardService;
    private final AuditService auditService;
    private final GarageScope garageScope;

    /** Issues the final report once a credit-closed job is finally paid; optional for hand-built test instances. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.modules.finalreport.service.FinalReportService finalReportService;

    @Override
    @Transactional
    public CreditAuthorizationResponse authorize(Long invoiceId, AuthorizeCreditRequest request) {

        GarageUserPrincipal principal = garageScope.principal();
        Invoice invoice = lockInvoice(invoiceId, principal);
        JobCard jobCard = invoice.getEstimate().getJobCard();
        requireOwnerOf(principal, jobCard);

        if (invoice.getInvoiceStatus() != InvoiceStatus.GENERATED && invoice.getInvoiceStatus() != InvoiceStatus.ACCEPTED) {
            throw new BusinessException("Credit can only be authorized on an issued invoice.", "CREDIT_INVOICE_NOT_ISSUED");
        }

        if (invoice.getPaymentStatus() == PaymentStatus.PAID || invoice.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("This invoice is already settled, so no credit is needed.", "CREDIT_INVOICE_SETTLED");
        }

        BigDecimal outstanding = outstanding(invoice);

        if (outstanding.signum() <= 0) {
            throw new BusinessException("This invoice has no outstanding balance.", "CREDIT_NO_OUTSTANDING");
        }

        BigDecimal amount = request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP);

        if (amount.signum() <= 0) {
            throw new BusinessException("The credit amount must be greater than zero.", "CREDIT_AMOUNT_INVALID");
        }

        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessException(
                    "The credit amount exceeds the outstanding balance of " + outstanding.toPlainString() + ".",
                    "CREDIT_AMOUNT_EXCEEDS_OUTSTANDING");
        }

        if (request.getDueDate() == null || !request.getDueDate().isAfter(LocalDate.now())) {
            throw new BusinessException("The credit due date must be in the future.", "CREDIT_DUE_DATE_INVALID");
        }

        if (creditRepository.existsByInvoiceIdAndStatus(invoiceId, CreditAuthorizationStatus.ACTIVE)) {
            throw new BusinessException("This invoice already has an active credit authorization.", "CREDIT_ALREADY_ACTIVE");
        }

        InvoiceCreditAuthorization credit = new InvoiceCreditAuthorization();
        credit.setGarageId(jobCard.getGarage().getId());
        credit.setInvoiceId(invoice.getId());
        credit.setJobCardId(jobCard.getId());
        credit.setStatus(CreditAuthorizationStatus.ACTIVE);
        credit.setAuthorizedAmount(amount);
        credit.setDueDate(request.getDueDate());
        credit.setReason(request.getReason().trim());
        credit.setAuthorizedByUserId(principal.getId());
        credit.setAuthorizedAt(LocalDateTime.now());
        credit = creditRepository.saveAndFlush(credit);

        audit(AuditEventType.CREDIT_AUTHORIZED, credit, principal, Map.of(
                "amount", amount.toPlainString(),
                "outstanding", outstanding.toPlainString(),
                "dueDate", credit.getDueDate().toString()));

        // Release clearance now holds, so a job that was only waiting for payment/credit becomes READY.
        if (jobCard.getStatus() == JobCardStatus.INVOICE_GENERATED) {
            jobCardService.readyForDelivery(jobCard.getJobCardNumber());
        }

        return toResponse(credit, invoice, outstanding);
    }

    @Override
    @Transactional
    public CreditAuthorizationResponse revoke(Long invoiceId, RevokeCreditRequest request) {

        GarageUserPrincipal principal = garageScope.principal();
        Invoice invoice = lockInvoice(invoiceId, principal);
        requireOwnerOf(principal, invoice.getEstimate().getJobCard());

        InvoiceCreditAuthorization credit = creditRepository
                .findFirstByInvoiceIdAndStatus(invoiceId, CreditAuthorizationStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "There is no active credit authorization to revoke.", "CREDIT_NOT_ACTIVE"));

        credit.setStatus(CreditAuthorizationStatus.REVOKED);
        credit.setRevokedAt(LocalDateTime.now());
        credit.setRevokedByUserId(principal.getId());
        credit.setRevokeReason(request.getReason().trim());
        credit = creditRepository.saveAndFlush(credit);

        audit(AuditEventType.CREDIT_REVOKED, credit, principal, Map.of("reason", credit.getRevokeReason()));

        return toResponse(credit, invoice, outstanding(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditAuthorizationResponse> history(Long invoiceId) {

        GarageUserPrincipal principal = garageScope.principal();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));
        requireGarageAccess(principal, invoice.getEstimate().getJobCard());

        BigDecimal outstanding = outstanding(invoice);

        return creditRepository.findByInvoiceIdOrderByIdDesc(invoiceId).stream()
                .map(c -> toResponse(c, invoice, outstanding))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditAuthorizationResponse> listActive() {

        GarageUserPrincipal principal = garageScope.principal();
        Set<Long> garageIds = garageScope.allowedGarageIds(principal);

        if (garageIds == null || garageIds.isEmpty()) {
            return List.of();
        }

        List<CreditAuthorizationResponse> out = new ArrayList<>();

        for (InvoiceCreditAuthorization c : creditRepository
                .findByGarageIdInAndStatusOrderByDueDateAsc(garageIds, CreditAuthorizationStatus.ACTIVE)) {
            Invoice invoice = invoiceRepository.findById(c.getInvoiceId()).orElse(null);
            if (invoice != null) {
                out.add(toResponse(c, invoice, outstanding(invoice)));
            }
        }

        return out;
    }

    @Override
    @Transactional
    public void onPaymentRecorded(Invoice invoice, BigDecimal amount, Long actorUserId) {

        if (invoice == null || invoice.getId() == null) {
            return;
        }

        creditRepository.findFirstByInvoiceIdAndStatus(invoice.getId(), CreditAuthorizationStatus.ACTIVE)
                .ifPresent(credit -> {

                    Map<String, Object> repayment = new LinkedHashMap<>();
                    repayment.put("amount", amount == null ? null : amount.toPlainString());
                    repayment.put("outstandingAfter", outstanding(invoice).toPlainString());
                    auditAs(AuditEventType.CREDIT_REPAYMENT, credit, actorUserId, repayment);

                    if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
                        credit.setStatus(CreditAuthorizationStatus.SETTLED);
                        credit.setSettledAt(LocalDateTime.now());
                        creditRepository.saveAndFlush(credit);
                        auditAs(AuditEventType.CREDIT_SETTLED, credit, actorUserId, Map.of());

                        // A job closed under credit had no final report (reports need PAID + delivered + CLOSED).
                        // Now that the invoice is paid, issue it; the call is idempotent per audience.
                        JobCard jobCard = invoice.getEstimate().getJobCard();
                        if (finalReportService != null && jobCard.getStatus() == JobCardStatus.CLOSED) {
                            finalReportService.generateOnClosure(jobCard.getId());
                        }
                    }
                });
    }

    // ------------------------------------------------------------------ helpers

    private Invoice lockInvoice(Long invoiceId, GarageUserPrincipal principal) {

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));

        requireGarageAccess(principal, invoice.getEstimate().getJobCard());

        return invoice;
    }

    /** Not-found (never forbidden) for another garage's invoice: its existence is not confirmed. */
    private void requireGarageAccess(GarageUserPrincipal principal, JobCard jobCard) {

        if (jobCard.getGarage() == null || !garageScope.canAccessGarage(principal, jobCard.getGarage().getId())) {
            throw new ResourceNotFoundException("Invoice not found.");
        }
    }

    private void requireOwnerOf(GarageUserPrincipal principal, JobCard jobCard) {

        requireGarageAccess(principal, jobCard);

        if (!garageScope.hasRole(principal, RoleCode.OWNER)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the garage Owner can authorize or revoke credit.");
        }
    }

    private BigDecimal outstanding(Invoice invoice) {

        BigDecimal paid = paymentRepository.sumAmountByInvoiceId(invoice.getId());

        return invoice.getGrandTotal()
                .subtract(paid == null ? BigDecimal.ZERO : paid)
                .max(BigDecimal.ZERO);
    }

    private void audit(AuditEventType type, InvoiceCreditAuthorization credit, GarageUserPrincipal actor,
                       Map<String, Object> extra) {
        auditAs(type, credit, actor.getId(), extra);
    }

    private void auditAs(AuditEventType type, InvoiceCreditAuthorization credit, Long actorUserId,
                         Map<String, Object> extra) {

        Map<String, Object> metadata = new LinkedHashMap<>(extra);
        metadata.put("invoiceId", credit.getInvoiceId());
        metadata.put("jobCardId", credit.getJobCardId());
        metadata.put("actorUserId", actorUserId);

        auditService.record(type, ENTITY, credit.getId(), credit.getGarageId(), metadata);
    }

    private CreditAuthorizationResponse toResponse(InvoiceCreditAuthorization c, Invoice invoice, BigDecimal outstanding) {

        JobCard jobCard = invoice.getEstimate().getJobCard();

        return CreditAuthorizationResponse.builder()
                .id(c.getId())
                .invoiceId(c.getInvoiceId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .jobCardId(c.getJobCardId())
                .jobCardNumber(jobCard.getJobCardNumber())
                .status(c.getStatus())
                .authorizedAmount(c.getAuthorizedAmount())
                .outstandingAmount(outstanding)
                .dueDate(c.getDueDate())
                .overdue(c.getStatus() == CreditAuthorizationStatus.ACTIVE && c.getDueDate().isBefore(LocalDate.now()))
                .reason(c.getReason())
                .authorizedByUserId(c.getAuthorizedByUserId())
                .authorizedAt(c.getAuthorizedAt())
                .revokedAt(c.getRevokedAt())
                .revokeReason(c.getRevokeReason())
                .settledAt(c.getSettledAt())
                .build();
    }
}
