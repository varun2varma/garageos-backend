package com.garageos.modules.invoice.service.impl;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.InvoiceNumberGenerator;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.invoice.dto.request.CreateInvoiceRequest;
import com.garageos.modules.invoice.dto.response.InvoiceResponse;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.mapper.InvoiceMapper;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.invoice.service.InvoiceService;
import com.garageos.modules.invoiceitem.entity.InvoiceItem;
import com.garageos.modules.invoiceitem.repository.InvoiceItemRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import org.springframework.transaction.annotation.Transactional;
import com.garageos.core.tax.TaxPolicy;
import com.garageos.modules.invoice.service.InvoiceTaxAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.garageos.modules.notification.event.NotificationEventPublisher;
import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.modules.notification.event.NotificationEvents;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    /** Membership-based tenant scope (field-injected so hand-built unit-test instances still work). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.core.security.GarageScope garageScope;

    /** Optional so constructor-built tests keep working; always present in the app. */
    @Autowired(required = false)
    private TaxPolicy taxPolicy;

    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    /** Optional so existing constructor-built tests are unaffected; always present in the app. */
    @Autowired(required = false)
    private com.garageos.modules.payment.repository.PaymentRepository paymentRepository;

    private final InvoiceRepository invoiceRepository;
    private final EstimateRepository estimateRepository;
    private final InvoiceMapper invoiceMapper;
    private final JobCardRepository jobCardRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final JobCardStatusValidator statusValidator;
    private final JobCardService jobCardService;
    private final CustomerRepository customerRepository;

    /**
     * Canonical invoice-generation JobCard transition, shared by every
     * invoice-creation entry point. Idempotent: a JobCard already at or
     * past INVOICE_GENERATED is left untouched rather than re-validated,
     * so a legacy entry point invoked after the canonical one already
     * ran does not fail on a redundant call.
     */
    private void transitionJobCardToInvoiceGenerated(JobCard jobCard) {

        if (jobCard.getStatus() == JobCardStatus.INVOICE_GENERATED) {
            return;
        }

        statusValidator.validate(
                jobCard.getStatus(),
                JobCardStatus.INVOICE_GENERATED
        );

        jobCard.setStatus(JobCardStatus.INVOICE_GENERATED);

        jobCardRepository.save(jobCard);
    }

    /**
     * Takes the estimate row lock and then re-reads the stored status (a scalar query, never answered from the
     * entity loaded before the lock), so an approval, rejection or edit committed while this request waited is seen.
     */
    private void requireApprovedUnderLock(Estimate estimate, String message) {

        estimateRepository.findByIdForUpdate(estimate.getId());

        EstimateStatus current = estimateRepository.findStatusById(estimate.getId()).orElse(estimate.getStatus());

        if (current != EstimateStatus.APPROVED) {
            throw new BusinessException(message);
        }
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {

        Estimate estimate = estimateRepository.findById(request.getEstimateId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : "
                                        + request.getEstimateId()));

        requireApprovedUnderLock(estimate, "Only approved estimates can be converted to invoice.");

        if (estimate.getStatus() != EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Only approved estimates can be converted to invoice.");
        }

        if (invoiceRepository.existsByEstimateId(estimate.getId())) {
            throw new BusinessException(
                    "Invoice already exists for this estimate.");
        }

        JobCard jobCard = estimate.getJobCard();

        authorizeInvoiceAction(jobCard);

        Long garageId = jobCard.getGarage().getId();

        Optional<Invoice> latestInvoice =
                invoiceRepository
                        .findTopByEstimateJobCardGarageIdOrderByIdDesc(garageId);

        String invoiceNumber =
                InvoiceNumberGenerator.generate(
                        jobCard.getGarage().getGarageCode(),
                        latestInvoice
                                .map(Invoice::getInvoiceNumber)
                                .orElse(null));

        Invoice invoice = invoiceMapper.toEntity(request);

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setEstimate(estimate);

        invoice.setInvoiceStatus(InvoiceStatus.GENERATED);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        List<InvoiceItem> snapshotItems = applyCanonicalSnapshot(invoice, estimate);

        invoice = invoiceRepository.save(invoice);

        invoiceItemRepository.saveAll(snapshotItems);

        transitionJobCardToInvoiceGenerated(jobCard);

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.INVOICE_GENERATED, jobCard, invoice.getId(), "1");

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoice(Long id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found with id : " + id));

        authorizeInvoiceAccess(invoice);

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getAllInvoices(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Never list invoices across garages or customers: staff see their
        // own garage invoices, a customer sees only their own, anyone else none.
        GarageUserPrincipal principal = currentPrincipal();

        if (principal.getGarageId() != null) {
            return invoiceRepository
                    .findByEstimateJobCardGarageId(principal.getGarageId(), pageable)
                    .map(invoiceMapper::toResponse);
        }

        return customerRepository.findByMobileNumber(principal.getMobile())
                .map(customer -> invoiceRepository
                        .findByEstimateJobCardCustomerId(customer.getId(), pageable)
                        .map(invoiceMapper::toResponse))
                .orElseGet(() -> Page.empty(pageable));
    }

//    @Override
//    public InvoiceResponse updateInvoice(
//            Long id,
//            CreateInvoiceRequest request) {
//
//        throw new UnsupportedOperationException(
//                "Invoice update is not allowed after creation.");
//    }

    /**
     * An issued invoice is a financial record: it is never physically deleted
     * (payments, items and reports reference it). "Delete" cancels an unpaid
     * invoice instead; one that has received any payment must be corrected
     * through a separate refund/adjustment workflow.
     */
    @Override
    @Transactional
    public void deleteInvoice(Long id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found with id : " + id));

        // Cancelling is a staff operation on the caller own garage only.
        authorizeInvoiceAction(invoice.getEstimate().getJobCard());

        if (invoice.getInvoiceStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Invoice is already cancelled.");
        }

        boolean hasPayments = (invoice.getPaymentStatus() != null && invoice.getPaymentStatus() != PaymentStatus.PENDING)
                || invoice.getInvoiceStatus() == InvoiceStatus.PAID
                || (paymentRepository != null
                && paymentRepository.sumAmountByInvoiceId(invoice.getId()) != null
                && paymentRepository.sumAmountByInvoiceId(invoice.getId()).signum() > 0);

        if (hasPayments) {
            throw new BusinessException(
                    "An invoice that has received payments cannot be cancelled. Use a refund or adjustment.");
        }

        invoice.setInvoiceStatus(InvoiceStatus.CANCELLED);

        invoiceRepository.save(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByInvoiceNumber(
            String invoiceNumber) {

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found : " + invoiceNumber));

        authorizeInvoiceAccess(invoice);

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        authorizeInvoiceAction(jobCard);

        Estimate estimate = estimateRepository
                .findByJobCardId(jobCard.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found for Job Card : "
                                        + jobCardNumber));

        requireApprovedUnderLock(estimate, "Estimate must be approved before invoice generation.");

        if (estimate.getStatus() != EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Estimate must be approved before invoice generation.");
        }

        if (invoiceRepository.existsByEstimateId(estimate.getId())) {
            throw new BusinessException(
                    "Invoice already exists for this Job Card.");
        }

        Long garageId = jobCard.getGarage().getId();

        Optional<Invoice> latestInvoice =
                invoiceRepository
                        .findTopByEstimateJobCardGarageIdOrderByIdDesc(garageId);

        String invoiceNumber =
                InvoiceNumberGenerator.generate(
                        jobCard.getGarage().getGarageCode(),
                        latestInvoice
                                .map(Invoice::getInvoiceNumber)
                                .orElse(null));

        Invoice invoice = new Invoice();

        invoice.setInvoiceNumber(invoiceNumber);

        invoice.setEstimate(estimate);

        invoice.setInvoiceStatus(InvoiceStatus.GENERATED);

        invoice.setPaymentStatus(PaymentStatus.PENDING);

        // Snapshot the FINAL billable lines (customer-selected only) and the
        // canonical tax figures so the invoice owns its own values; removed
        // items never appear and later catalog or rule edits cannot change it.
        List<InvoiceItem> snapshotItems = applyCanonicalSnapshot(invoice, estimate);

        invoice.setGeneratedAt(LocalDateTime.now());
        invoice = invoiceRepository.save(invoice);

        invoiceItemRepository.saveAll(snapshotItems);

        transitionJobCardToInvoiceGenerated(jobCard);

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.INVOICE_GENERATED, jobCard, invoice.getId(), "1");

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse generateInvoiceByEstimateId(Long estimateId) {

        Estimate estimate = estimateRepository.findById(estimateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found"));

        requireApprovedUnderLock(estimate, "Only approved estimates can generate invoices.");

        if (estimate.getStatus() != EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Only approved estimates can generate invoices.");
        }

        if (invoiceRepository.existsByEstimateId(estimateId)) {
            throw new BusinessException(
                    "Invoice already generated.");
        }

        JobCard jobCard = estimate.getJobCard();

        authorizeInvoiceAction(jobCard);

        Long garageId = jobCard.getGarage().getId();

        Optional<Invoice> latestInvoice =
                invoiceRepository
                        .findTopByEstimateJobCardGarageIdOrderByIdDesc(garageId);

        String invoiceNumber =
                InvoiceNumberGenerator.generate(
                        jobCard.getGarage().getGarageCode(),
                        latestInvoice
                                .map(Invoice::getInvoiceNumber)
                                .orElse(null));

        Invoice invoice = new Invoice();

        invoice.setInvoiceNumber(invoiceNumber);

        invoice.setEstimate(estimate);

        invoice.setInvoiceStatus(InvoiceStatus.DRAFT);

        invoice.setPaymentStatus(PaymentStatus.PENDING);

        List<InvoiceItem> snapshotItems = applyCanonicalSnapshot(invoice, estimate);

        invoice.setRemarks(estimate.getRemarks());

        invoice.setGeneratedAt(LocalDateTime.now());

        invoice = invoiceRepository.save(invoice);

        invoiceItemRepository.saveAll(snapshotItems);

        transitionJobCardToInvoiceGenerated(jobCard);

        return invoiceMapper.toResponse(invoice);
    }

    private TaxPolicy taxPolicy() {
        return taxPolicy != null ? taxPolicy : TaxPolicy.legacyCompatible();
    }

    /**
     * Computes the canonical tax snapshot for a new invoice (header totals set on
     * {@code invoice}, unsaved lines returned) from the estimate's customer-kept
     * lines. A persisted estimate total that disagrees with the recomputation is
     * logged, never trusted: the invoice always carries the recomputed figures.
     */
    private List<InvoiceItem> applyCanonicalSnapshot(Invoice invoice, Estimate estimate) {

        List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateId(estimate.getId());

        JobCard jobCard = estimate.getJobCard();

        List<InvoiceItem> lines = InvoiceTaxAssembler.assemble(
                invoice, estimate, estimateItems,
                jobCard == null ? null : jobCard.getGarage(),
                jobCard == null ? null : jobCard.getCustomer(),
                taxPolicy());

        if (estimate.getGrandTotal() != null
                && estimate.getGrandTotal().compareTo(invoice.getGrandTotal()) != 0) {
            log.warn("Estimate {} stored total {} differs from the recomputed invoice total {}; using the recomputed total.",
                    estimate.getId(), estimate.getGrandTotal(), invoice.getGrandTotal());
        }

        return lines;
    }

    /**
     * Canonical payment transition: Invoice.paymentStatus is the
     * authoritative payment record (never JobCardStatus.PAYMENT_PENDING/
     * PAYMENT_COMPLETED, which remain unused legacy enum values). A
     * successful payment atomically advances the JobCard to
     * READY_FOR_DELIVERY via the same validated transition
     * JobCardServiceImpl.readyForDelivery already implements — this is
     * what makes INVOICE_GENERATED -> READY_FOR_DELIVERY a validator-legal
     * transition instead of the previously-confirmed contradiction where
     * ServiceWorkflowServiceImpl called this out-of-band after the fact.
     */
    @Override
    @Transactional
    public InvoiceResponse receivePayment(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));
//        commenting this for MVP purpose when the payment gateway is been setup we need to open this flow.
        authorizePaymentAction(jobCard);


        Invoice invoice = invoiceRepository
                .findByEstimateJobCardId(jobCard.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found for Job Card : "
                                        + jobCardNumber));

        if (invoice.getInvoiceStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException(
                    "This invoice has been cancelled and cannot receive payments.");
        }

        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException(
                    "Invoice already paid.");
        }

        /*
         * New gate, CUSTOMER callers only: Invoice Generated -> Customer
         * accepts (acceptInvoice) -> Payment becomes available. Staff
         * callers (MANAGER/SERVICE_ADVISOR/OWNER, e.g. recording an
         * in-person/cash payment at the counter) are deliberately exempt
         * so the pre-existing staff payment flow is not blocked on the
         * customer ever having opened the app.
         */
        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (principal.getRoles().contains(RoleCode.CUSTOMER.name())
                && invoice.getInvoiceStatus() != InvoiceStatus.ACCEPTED) {

            throw new BusinessException(
                    "Invoice must be accepted before payment can be received.");
        }

        invoice.setPaymentStatus(PaymentStatus.PAID);

        recordSettlementPayment(invoice, jobCard, principal);

        /*
         * saveAndFlush (not save) so a lost optimistic-lock race is
         * detected synchronously, right here, rather than deferred to
         * commit - where it would surface outside this try/catch as an
         * opaque 500. Invoice.version (V55) is what makes this race
         * detectable at all: two concurrent requests could otherwise both
         * read PENDING and both write PAID.
         */
        try {
            invoice = invoiceRepository.saveAndFlush(invoice);
        } catch (ObjectOptimisticLockingFailureException raced) {
            throw new BusinessException("Invoice already paid.");
        }

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.PAYMENT_RECEIVED, jobCard, invoice.getId(), "1");

        if (jobCard.getStatus() != JobCardStatus.READY_FOR_DELIVERY) {
            jobCardService.readyForDelivery(jobCardNumber);
        }

        return invoiceMapper.toResponse(invoice);
    }

    /**
     * The legacy "mark as paid" flow settles whatever balance is left. It
     * now also writes that settlement into the payment ledger (method not
     * captured, hence UNSPECIFIED) so collection reports never miss it.
     * Existing behaviour and responses are unchanged.
     */
    private void recordSettlementPayment(
            Invoice invoice, JobCard jobCard, GarageUserPrincipal principal) {

        if (paymentRepository == null || jobCard.getGarage() == null) {
            return;
        }

        java.math.BigDecimal alreadyPaid = paymentRepository.sumAmountByInvoiceId(invoice.getId());
        java.math.BigDecimal remaining = invoice.getGrandTotal().subtract(
                alreadyPaid == null ? java.math.BigDecimal.ZERO : alreadyPaid);

        if (remaining.signum() <= 0) {
            return;
        }

        com.garageos.modules.payment.entity.Payment payment =
                new com.garageos.modules.payment.entity.Payment();
        payment.setInvoice(invoice);
        payment.setGarageId(jobCard.getGarage().getId());
        payment.setAmount(remaining);
        payment.setMethod(com.garageos.core.enums.payment.PaymentMethod.UNSPECIFIED);
        payment.setReceivedByUserId(principal.getId());
        payment.setPaidAt(LocalDateTime.now());
        payment.setStatus(com.garageos.core.enums.payment.PaymentRecordStatus.COMPLETED);

        paymentRepository.save(payment);
    }

    /**
     * Shared CUSTOMER-ownership check, extracted from what was previously
     * only inlined in authorizePaymentAction: a CUSTOMER principal is not
     * a garage employee (no meaningful principal.getGarageId() to
     * compare), so the garage-match check does not apply to them at all;
     * they are authorized instead by owning the Job Card itself, the same
     * Customer-by-mobile-number lookup JobCardProjectionServiceImpl
     * already uses for the customer job-card view. A customer who does
     * not own this Job Card gets the same ResourceNotFoundException the
     * rest of the app already uses to avoid confirming another
     * customer's Job Card/invoice exists. Now reused by both
     * acceptInvoice() and authorizePaymentAction() so the two customer
     * self-service steps enforce ownership identically.
     */
    private void authorizeCustomerOwnsJobCard(
            JobCard jobCard,
            GarageUserPrincipal principal) {

        Customer customer = customerRepository
                .findByMobileNumber(principal.getMobile())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : "
                                        + jobCard.getJobCardNumber()));

        if (jobCard.getCustomer() == null
                || !jobCard.getCustomer().getId().equals(customer.getId())) {

            throw new ResourceNotFoundException(
                    "Job Card not found : " + jobCard.getJobCardNumber());
        }
    }

    /**
     * receivePayment()'s own authorization - deliberately separate from
     * authorizeInvoiceAction(), which stays exactly as-is for
     * generateInvoice() and every staff caller.
     *
     * Identity-first, not role-first: the branch used to be "does this
     * principal currently hold the CUSTOMER role" - correct in the common
     * case, but it makes payment authorization depend on role-assignment
     * state (assignCustomerRole()/activateCustomer()) rather than on the
     * canonical Customer<->JobCard relationship the domain already has.
     * This now asks the real question directly: is this principal
     * (by mobile number) the Customer this Job Card actually belongs to?
     * If so, that is authorization on its own - regardless of whatever
     * roles happen to be assigned to their account right now. Only a
     * principal with NO matching Customer record at all (i.e. genuinely
     * not a customer) falls through to the garage-membership check, which
     * remains completely unchanged for staff. This does not weaken
     * anything: a mismatched customer (mobile matches a Customer, but not
     * this Job Card's Customer) still gets rejected exactly as before,
     * with the same not-found response used everywhere else to avoid
     * confirming another customer's data exists.
     */
    private void authorizePaymentAction(JobCard jobCard) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        Optional<Customer> maybeCustomer =
                customerRepository.findByMobileNumber(principal.getMobile());

        if (maybeCustomer.isPresent()) {

            Customer customer = maybeCustomer.get();

            if (jobCard.getCustomer() == null
                    || !jobCard.getCustomer().getId().equals(customer.getId())) {

                throw new ResourceNotFoundException(
                        "Job Card not found : " + jobCard.getJobCardNumber());
            }
        } else {
            authorizeInvoiceAction(jobCard);
        }
    }

    /**
     * CUSTOMER-only acceptance step inserted between invoice generation
     * and payment (Invoice Generated -> Customer accepts -> Payment
     * becomes available). Role is already enforced at the controller
     * (@PreAuthorize hasRole('CUSTOMER')); this method enforces ownership
     * of the specific Job Card via authorizeCustomerOwnsJobCard, the same
     * check receivePayment() already relies on for its CUSTOMER branch.
     * Idempotent: re-accepting an already-ACCEPTED invoice is a no-op
     * that returns the current state rather than erroring, so a retried
     * request (e.g. after a dropped response) does not surface a
     * confusing failure. Uses saveAndFlush + @Version, mirroring
     * receivePayment()'s own optimistic-locking race handling, so two
     * concurrent accept requests cannot both "win".
     */
    @Override
    @Transactional
    public InvoiceResponse acceptInvoice(String jobCardNumber) {

        JobCard jobCard = jobCardRepository
                .findByJobCardNumber(jobCardNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Job Card not found : " + jobCardNumber));

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        authorizeCustomerOwnsJobCard(jobCard, principal);

        Invoice invoice = invoiceRepository
                .findByEstimateJobCardId(jobCard.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found for Job Card : "
                                        + jobCardNumber));

        if (invoice.getInvoiceStatus() == InvoiceStatus.ACCEPTED) {
            return invoiceMapper.toResponse(invoice);
        }

        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Invoice already paid.");
        }

        if (invoice.getInvoiceStatus() != InvoiceStatus.GENERATED) {
            throw new BusinessException(
                    "Invoice must be generated before it can be accepted.");
        }

        invoice.setInvoiceStatus(InvoiceStatus.ACCEPTED);

        try {
            invoice = invoiceRepository.saveAndFlush(invoice);
        } catch (ObjectOptimisticLockingFailureException raced) {
            throw new BusinessException("Invoice already accepted.");
        }

        NotificationEvents.publishJobCardEvent(notificationEventPublisher,
                NotificationEventType.INVOICE_ACCEPTED, invoice.getEstimate().getJobCard(), invoice.getId(), "1");

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByJobCard(Long jobCardId) {

        return invoiceRepository
                .findByEstimateJobCardId(jobCardId)
                .map(invoice -> {
                    authorizeInvoiceAccess(invoice);
                    return invoiceMapper.toResponse(invoice);
                })
                .orElse(null);
    }

    private GarageUserPrincipal currentPrincipal() {

        return (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    /**
     * Read access to ONE invoice: staff of the invoice garage, or the
     * customer who owns its job card. Anyone else gets the same not-found
     * used elsewhere, so another garage or customer invoice is never
     * confirmed to exist just because an id was guessed.
     */
    private void authorizeInvoiceAccess(Invoice invoice) {

        JobCard jobCard = invoice.getEstimate().getJobCard();

        GarageUserPrincipal principal = currentPrincipal();

        if (jobCard.getGarage() != null
                && com.garageos.core.security.GarageScope.isMemberOfGarage(garageScope, principal, jobCard.getGarage().getId())) {
            return;
        }

        if (principal.getMobile() != null && jobCard.getCustomer() != null) {

            Optional<Customer> customer =
                    customerRepository.findByMobileNumber(principal.getMobile());

            if (customer.isPresent()
                    && jobCard.getCustomer().getId().equals(customer.get().getId())) {
                return;
            }
        }

        throw new ResourceNotFoundException("Invoice not found.");
    }

    /**
     * Corrective fix for Defect #7: generateInvoice()/receivePayment()
     * relied solely on @PreAuthorize(WORKFLOW_OPERATIONAL_ROLES) at the
     * controller (role-only, no tenant scoping), so a MANAGER/OWNER/
     * SERVICE_ADVISOR from any garage could generate an invoice or record
     * payment for any other garage's JobCard - confirmed live against
     * Postgres. Mirrors the garage-match check already used successfully
     * in RepairTaskServiceImpl and QualityCheckServiceImpl.
     */
    private void authorizeInvoiceAction(JobCard jobCard) {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        if (jobCard.getGarage() == null
                || !com.garageos.core.security.GarageScope.isMemberOfGarage(garageScope, principal, jobCard.getGarage().getId())) {

            throw new BusinessException(
                    "E3: This Job Card does not belong to your garage.");
        }
    }

}