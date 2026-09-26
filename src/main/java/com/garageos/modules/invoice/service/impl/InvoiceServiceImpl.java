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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

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

    @Override
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {

        Estimate estimate = estimateRepository.findById(request.getEstimateId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found with id : "
                                        + request.getEstimateId()));

        if (estimate.getStatus() != EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Only approved estimates can be converted to invoice.");
        }

        if (invoiceRepository.existsByEstimateId(estimate.getId())) {
            throw new BusinessException(
                    "Invoice already exists for this estimate.");
        }

        JobCard jobCard = estimate.getJobCard();

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

        invoice.setSubtotal(estimate.getSubtotal());
        invoice.setDiscount(estimate.getDiscount());
        invoice.setGst(estimate.getGst());
        invoice.setGrandTotal(estimate.getGrandTotal());

        invoice = invoiceRepository.save(invoice);

        transitionJobCardToInvoiceGenerated(jobCard);

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoice(Long id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found with id : " + id));

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

        return invoiceRepository.findAll(pageable)
                .map(invoiceMapper::toResponse);
    }

//    @Override
//    public InvoiceResponse updateInvoice(
//            Long id,
//            CreateInvoiceRequest request) {
//
//        throw new UnsupportedOperationException(
//                "Invoice update is not allowed after creation.");
//    }

    @Override
    public void deleteInvoice(Long id) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found with id : " + id));

        invoiceRepository.delete(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByInvoiceNumber(
            String invoiceNumber) {

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found : " + invoiceNumber));

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

        invoice.setSubtotal(estimate.getSubtotal());

        invoice.setDiscount(estimate.getDiscount());

        invoice.setGst(estimate.getGst());

        invoice.setGrandTotal(estimate.getGrandTotal());
        invoice.setGeneratedAt(LocalDateTime.now());
        invoice = invoiceRepository.save(invoice);

        transitionJobCardToInvoiceGenerated(jobCard);

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse generateInvoiceByEstimateId(Long estimateId) {

        Estimate estimate = estimateRepository.findById(estimateId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estimate not found"));

        if (estimate.getStatus() != EstimateStatus.APPROVED) {
            throw new BusinessException(
                    "Only approved estimates can generate invoices.");
        }

        if (invoiceRepository.existsByEstimateId(estimateId)) {
            throw new BusinessException(
                    "Invoice already generated.");
        }

        JobCard jobCard = estimate.getJobCard();

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

        invoice.setSubtotal(estimate.getSubtotal());

        invoice.setDiscount(estimate.getDiscount());

        invoice.setGst(estimate.getGst());

        invoice.setGrandTotal(estimate.getGrandTotal());

        invoice.setRemarks(estimate.getRemarks());

        invoice.setGeneratedAt(LocalDateTime.now());

        invoice = invoiceRepository.save(invoice);

        copyEstimateItems(invoice, estimate);

        transitionJobCardToInvoiceGenerated(jobCard);

        return invoiceMapper.toResponse(invoice);
    }

    private void copyEstimateItems(
            Invoice invoice,
            Estimate estimate) {

        List<EstimateItem> estimateItems =
                estimateItemRepository.findByEstimateId(estimate.getId());

        List<InvoiceItem> invoiceItems = new ArrayList<>();

        for (EstimateItem estimateItem : estimateItems) {

            InvoiceItem invoiceItem = new InvoiceItem();

            invoiceItem.setInvoice(invoice);

            invoiceItem.setComplaint(estimateItem.getComplaint());

//            invoiceItem.setInspectionFinding(
//                    estimateItem.getInspectionFinding());

            invoiceItem.setItemType(estimateItem.getItemType());

            invoiceItem.setDescription(estimateItem.getDescription());

            invoiceItem.setQuantity(estimateItem.getQuantity());

            invoiceItem.setUnitPrice(estimateItem.getUnitPrice());

            invoiceItem.setTotalPrice(estimateItem.getTotalPrice());

            invoiceItems.add(invoiceItem);
        }

        invoiceItemRepository.saveAll(invoiceItems);
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
//        authorizePaymentAction(jobCard);


        Invoice invoice = invoiceRepository
                .findByEstimateJobCardId(jobCard.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice not found for Job Card : "
                                        + jobCardNumber));

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

        if (jobCard.getStatus() != JobCardStatus.READY_FOR_DELIVERY) {
            jobCardService.readyForDelivery(jobCardNumber);
        }

        return invoiceMapper.toResponse(invoice);
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

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByJobCard(Long jobCardId) {

        return invoiceRepository
                .findByEstimateJobCardId(jobCardId)
                .map(invoiceMapper::toResponse)
                .orElse(null);
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
                || principal.getGarageId() == null
                || !principal.getGarageId().equals(jobCard.getGarage().getId())) {

            throw new BusinessException(
                    "This Job Card does not belong to your garage.");
        }
    }

}