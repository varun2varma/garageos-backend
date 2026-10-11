package com.garageos.modules.payment.service.impl;

import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.core.enums.payment.PaymentMethod;
import com.garageos.core.enums.payment.PaymentRecordStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.notification.event.NotificationEventPublisher;
import com.garageos.modules.notification.event.NotificationEvents;
import com.garageos.modules.payment.dto.request.RecordPaymentRequest;
import com.garageos.modules.payment.dto.response.FinanceInvoiceResponse;
import com.garageos.modules.payment.dto.response.PaymentResponse;
import com.garageos.modules.payment.dto.response.PaymentSummaryResponse;
import com.garageos.modules.payment.entity.Payment;
import com.garageos.modules.payment.repository.PaymentRepository;
import com.garageos.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);

    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final JobCardService jobCardService;
    private final GarageScope garageScope;

    // ------------------------------------------------------------------
    // Record
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest request) {

        if (request.getMethod() == null || !request.getMethod().isSelectable()) {
            throw new BusinessException("Please choose a payment method.");
        }

        GarageUserPrincipal principal = garageScope.principal();

        // Lock the invoice first: concurrent submissions for the same invoice
        // (including identical retries) serialise here, so the idempotency
        // and outstanding checks below are race-free.
        Invoice invoice = invoiceRepository.findByIdForUpdate(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));

        JobCard jobCard = invoice.getEstimate().getJobCard();

        if (jobCard.getGarage() == null
                || !garageScope.canAccessGarage(principal, jobCard.getGarage().getId())) {
            // Same not-found used elsewhere: never confirm another garage's invoice exists.
            throw new ResourceNotFoundException("Invoice not found.");
        }

        if (invoice.getInvoiceStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("This invoice has been cancelled and cannot receive payments.");
        }

        String key = blankToNull(request.getIdempotencyKey());

        if (key != null) {

            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(key);

            if (existing.isPresent()) {

                if (!existing.get().getInvoice().getId().equals(invoice.getId())) {
                    throw new BusinessException(
                            "This payment reference was already used for a different invoice.");
                }

                return toResponse(existing.get(), invoice, currentPaid(invoice),
                        userNames(List.of(existing.get())));
            }
        }

        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Invoice already paid.");
        }

        if (invoice.getInvoiceStatus() != InvoiceStatus.GENERATED
                && invoice.getInvoiceStatus() != InvoiceStatus.ACCEPTED) {
            throw new BusinessException("Payment can only be collected on an issued invoice.");
        }

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (amount.signum() <= 0) {
            throw new BusinessException("Amount must be greater than zero.");
        }

        BigDecimal alreadyPaid = currentPaid(invoice);
        BigDecimal outstanding = invoice.getGrandTotal().subtract(alreadyPaid);

        if (outstanding.signum() <= 0) {
            throw new BusinessException("Invoice already paid.");
        }

        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessException(
                    "Amount exceeds the outstanding balance of " + outstanding.toPlainString() + ".");
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setGarageId(jobCard.getGarage().getId());
        payment.setAmount(amount);
        payment.setMethod(request.getMethod());
        payment.setReference(blankToNull(request.getReference()));
        payment.setReceivedByUserId(principal.getId());
        payment.setPaidAt(LocalDateTime.now());
        payment.setStatus(PaymentRecordStatus.COMPLETED);
        payment.setIdempotencyKey(key);

        payment = paymentRepository.saveAndFlush(payment);

        BigDecimal paidNow = alreadyPaid.add(amount);
        boolean fullyPaid = paidNow.compareTo(invoice.getGrandTotal()) >= 0;

        invoice.setPaymentStatus(fullyPaid ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID);
        invoiceRepository.saveAndFlush(invoice);

        if (fullyPaid) {

            NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                    NotificationEventType.PAYMENT_RECEIVED, jobCard, invoice.getId(), "1");

            if (jobCard.getStatus() != JobCardStatus.READY_FOR_DELIVERY) {
                jobCardService.readyForDelivery(jobCard.getJobCardNumber());
            }
        }

        return toResponse(payment, invoice, paidNow, userNames(List.of(payment)));
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    @Override
    public PaymentResponse getPayment(Long id) {

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));

        GarageUserPrincipal principal = garageScope.principal();

        if (!garageScope.canAccessGarage(principal, payment.getGarageId())) {
            throw new ResourceNotFoundException("Payment not found.");
        }

        Invoice invoice = payment.getInvoice();

        return toResponse(payment, invoice, currentPaid(invoice), userNames(List.of(payment)));
    }

    @Override
    public List<PaymentResponse> getInvoicePayments(Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));

        requireInvoiceReadAccess(invoice);

        List<Payment> payments =
                paymentRepository.findByInvoiceIdOrderByPaidAtDescIdDesc(invoiceId);

        BigDecimal paid = currentPaid(invoice);
        Map<Long, String> names = userNames(payments);

        return payments.stream()
                .map(p -> toResponse(p, invoice, paid, names))
                .toList();
    }

    @Override
    public Page<PaymentResponse> getPayments(
            Long garageId, boolean allGarages, LocalDate from, LocalDate to,
            PaymentMethod method, int page, int size) {

        Set<Long> garageIds = resolveGarageIds(garageId, allGarages);

        LocalDateTime start = startOf(from == null ? EARLIEST : from);
        LocalDateTime end = endOf(to == null ? LocalDate.now() : to);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Direction.DESC, "paidAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Payment> payments = method == null
                ? paymentRepository.search(garageIds, start, end, pageable)
                : paymentRepository.searchByMethod(garageIds, start, end, method, pageable);

        Map<Long, String> names = userNames(payments.getContent());
        Map<Long, BigDecimal> paidByInvoice = paidTotals(
                payments.getContent().stream().map(p -> p.getInvoice().getId()).toList());

        return payments.map(p -> toResponse(p, p.getInvoice(),
                paidByInvoice.getOrDefault(p.getInvoice().getId(), BigDecimal.ZERO), names));
    }

    @Override
    public PaymentSummaryResponse getSummary(
            Long garageId, boolean allGarages, LocalDate from, LocalDate to) {

        Set<Long> garageIds = resolveGarageIds(garageId, allGarages);

        LocalDate fromDate = from == null ? LocalDate.now() : from;
        LocalDate toDate = to == null ? fromDate : to;

        if (toDate.isBefore(fromDate)) {
            throw new BusinessException("The end date must not be before the start date.");
        }

        LocalDateTime start = startOf(fromDate);
        LocalDateTime end = endOf(toDate);

        List<PaymentSummaryResponse.MethodTotal> byMethod = new ArrayList<>();
        BigDecimal collected = BigDecimal.ZERO;
        long count = 0;

        for (Object[] row : paymentRepository.summarizeByMethod(garageIds, start, end)) {

            long rowCount = ((Number) row[1]).longValue();
            BigDecimal rowTotal = (BigDecimal) row[2];

            byMethod.add(PaymentSummaryResponse.MethodTotal.builder()
                    .method(((PaymentMethod) row[0]).name())
                    .count(rowCount)
                    .total(rowTotal)
                    .build());

            collected = collected.add(rowTotal);
            count += rowCount;
        }

        byMethod.sort(Comparator.comparing(PaymentSummaryResponse.MethodTotal::getTotal).reversed());

        Object[] outstandingRow = invoiceRepository.outstandingTotals(garageIds).get(0);
        long outstandingCount = ((Number) outstandingRow[0]).longValue();
        BigDecimal outstandingGross = (BigDecimal) outstandingRow[1];
        BigDecimal outstanding = outstandingGross
                .subtract(paymentRepository.sumPaidOnPartiallyPaidInvoices(garageIds));

        Object[] issued = invoiceRepository.issuedTotals(garageIds, start, end).get(0);

        return PaymentSummaryResponse.builder()
                .from(fromDate)
                .to(toDate)
                .collectedTotal(collected)
                .paymentCount(count)
                .byMethod(byMethod)
                .outstandingInvoiceCount(outstandingCount)
                .outstandingAmount(outstanding)
                .invoicesIssuedCount(((Number) issued[0]).longValue())
                .invoicesSubtotal((BigDecimal) issued[1])
                .invoicesDiscount((BigDecimal) issued[2])
                .invoicesGst((BigDecimal) issued[3])
                .invoicesGrandTotal((BigDecimal) issued[4])
                .build();
    }

    @Override
    public Page<FinanceInvoiceResponse> getInvoices(
            Long garageId, boolean allGarages, boolean payableOnly,
            PaymentStatus paymentStatus, int page, int size) {

        Set<Long> garageIds = resolveGarageIds(garageId, allGarages);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "id"));

        Page<Invoice> invoices;

        if (payableOnly) {
            invoices = invoiceRepository
                    .findByEstimateJobCardGarageIdInAndInvoiceStatusInAndPaymentStatusIn(
                            garageIds,
                            List.of(InvoiceStatus.GENERATED, InvoiceStatus.ACCEPTED),
                            List.of(PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID),
                            pageable);
        } else if (paymentStatus != null) {
            invoices = invoiceRepository.findByEstimateJobCardGarageIdInAndPaymentStatus(
                    garageIds, paymentStatus, pageable);
        } else {
            invoices = invoiceRepository.findByEstimateJobCardGarageIdIn(garageIds, pageable);
        }

        Map<Long, BigDecimal> paid = paidTotals(
                invoices.getContent().stream().map(Invoice::getId).toList());

        return invoices.map(i -> toFinanceRow(i, paid.getOrDefault(i.getId(), BigDecimal.ZERO)));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Garages a finance read may cover: one explicit/own garage, or - for
     * an OWNER only - all of their active garages. Everything is checked
     * against GarageScope, never trusted from the request.
     */
    private Set<Long> resolveGarageIds(Long garageId, boolean allGarages) {

        GarageUserPrincipal principal = garageScope.principal();

        if (garageId == null && allGarages && garageScope.hasRole(principal, RoleCode.OWNER)) {

            Set<Long> ids = garageScope.allowedGarageIds(principal);

            if (ids.isEmpty()) {
                throw new BusinessException("No active garages found for this owner.");
            }

            return ids;
        }

        return Set.of(garageScope.resolveGarageId(principal, garageId));
    }

    private void requireInvoiceReadAccess(Invoice invoice) {

        GarageUserPrincipal principal = garageScope.principal();
        JobCard jobCard = invoice.getEstimate().getJobCard();

        boolean staff = !garageScope.isCustomerOnly(principal)
                && jobCard.getGarage() != null
                && garageScope.canAccessGarage(principal, jobCard.getGarage().getId());

        boolean ownCustomer = jobCard.getCustomer() != null
                && principal.getMobile() != null
                && principal.getMobile().equals(jobCard.getCustomer().getMobileNumber());

        if (!staff && !ownCustomer) {
            throw new ResourceNotFoundException("Invoice not found.");
        }
    }

    private BigDecimal currentPaid(Invoice invoice) {
        return paymentRepository.sumAmountByInvoiceId(invoice.getId());
    }

    private Map<Long, BigDecimal> paidTotals(Collection<Long> invoiceIds) {

        if (invoiceIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, BigDecimal> result = new HashMap<>();

        for (Object[] row : paymentRepository.sumAmountByInvoiceIds(new HashSet<>(invoiceIds))) {
            result.put((Long) row[0], (BigDecimal) row[1]);
        }

        return result;
    }

    private Map<Long, String> userNames(Collection<Payment> payments) {

        Set<Long> ids = payments.stream()
                .map(Payment::getReceivedByUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, PaymentServiceImpl::fullName));
    }

    private static String fullName(User user) {

        String last = user.getLastName();

        return last == null || last.isBlank()
                ? user.getFirstName()
                : user.getFirstName() + " " + last;
    }

    private PaymentResponse toResponse(
            Payment p, Invoice invoice, BigDecimal paidTotal, Map<Long, String> names) {

        JobCard jobCard = invoice.getEstimate().getJobCard();
        BigDecimal outstanding = invoice.getGrandTotal().subtract(paidTotal).max(BigDecimal.ZERO);

        return PaymentResponse.builder()
                .id(p.getId())
                .receiptNumber(String.format("RCP-%06d", p.getId()))
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .jobCardNumber(jobCard.getJobCardNumber())
                .customerName(jobCard.getCustomer() == null ? null : jobCard.getCustomer().getFullName())
                .vehicleRegistration(jobCard.getVehicle() == null
                        ? null : jobCard.getVehicle().getRegistrationNumber())
                .garageName(jobCard.getGarage() == null ? null : jobCard.getGarage().getGarageName())
                .amount(p.getAmount())
                .method(p.getMethod().name())
                .reference(p.getReference())
                .receivedByUserId(p.getReceivedByUserId())
                .receivedByName(p.getReceivedByUserId() == null
                        ? null : names.get(p.getReceivedByUserId()))
                .paidAt(p.getPaidAt())
                .status(p.getStatus().name())
                .invoiceGrandTotal(invoice.getGrandTotal())
                .invoicePaidTotal(paidTotal)
                .invoiceOutstanding(outstanding)
                .invoicePaymentStatus(invoice.getPaymentStatus().name())
                .build();
    }

    private FinanceInvoiceResponse toFinanceRow(Invoice i, BigDecimal paid) {

        JobCard jobCard = i.getEstimate().getJobCard();

        return FinanceInvoiceResponse.builder()
                .invoiceId(i.getId())
                .invoiceNumber(i.getInvoiceNumber())
                .jobCardNumber(jobCard.getJobCardNumber())
                .customerName(jobCard.getCustomer() == null ? null : jobCard.getCustomer().getFullName())
                .vehicleRegistration(jobCard.getVehicle() == null
                        ? null : jobCard.getVehicle().getRegistrationNumber())
                .invoiceStatus(i.getInvoiceStatus().name())
                .paymentStatus(i.getPaymentStatus().name())
                .subtotal(i.getSubtotal())
                .discount(i.getDiscount())
                .gst(i.getGst())
                .grandTotal(i.getGrandTotal())
                .paidAmount(paid)
                .outstandingAmount(i.getGrandTotal().subtract(paid).max(BigDecimal.ZERO))
                .generatedAt(i.getGeneratedAt())
                .build();
    }

    private static LocalDateTime startOf(LocalDate d) {
        return d.atStartOfDay();
    }

    /** Exclusive upper bound: the start of the day after {@code d}. */
    private static LocalDateTime endOf(LocalDate d) {
        return d.plusDays(1).atStartOfDay();
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
