package com.garageos.modules.invoice.service.impl;

import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.util.InvoiceTaxCalculator;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimateitem.entity.EstimateItem;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.garage.dto.response.GarageBrandingResponse;
import com.garageos.modules.garage.service.GarageBrandingService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.pdf.InvoiceDocumentData;
import com.garageos.modules.invoice.pdf.InvoiceSnapshotView;
import com.garageos.modules.invoice.pdf.InvoicePdfRenderer;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.invoice.service.InvoicePdfService;
import com.garageos.modules.invoiceitem.entity.InvoiceItem;
import com.garageos.modules.invoiceitem.repository.InvoiceItemRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoicePdfServiceImpl implements InvoicePdfService {

    /** Membership-based tenant scope (field-injected so hand-built unit-test instances still work). */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.core.security.GarageScope garageScope;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final CustomerRepository customerRepository;
    private final GarageBrandingService garageBrandingService;
    private final InvoicePdfRenderer renderer;

    @Override
    @Transactional(readOnly = true)
    public InvoicePdf generate(GarageUserPrincipal principal, Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found."));

        Estimate estimate = invoice.getEstimate();
        JobCard jobCard = estimate.getJobCard();

        authorize(principal, jobCard);

        // Authorization runs first so a caller outside the garage / not the owning
        // customer learns nothing about the invoice's payment state.
        requireFullyPaid(invoice);

        // JobCard -> garageId -> Garage -> branding. Never the viewer.
        Long garageId = jobCard.getGarage().getId();

        requireGarageAuthorization(principal, garageId, invoice);

        byte[] signatureBytes = null;
        byte[] stampBytes = null;

        if (authorizationAssets != null) {
            // loaded by the INVOICE's garage id (never the caller's); absent only for exempt legacy invoices
            signatureBytes = authorizationAssets.load(
                    garageId, com.garageos.modules.garage.service.GarageAuthorizationAssetService.Kind.SIGNATURE).orElse(null);
            stampBytes = authorizationAssets.load(
                    garageId, com.garageos.modules.garage.service.GarageAuthorizationAssetService.Kind.STAMP).orElse(null);
        }

        GarageBrandingResponse garage = garageBrandingService.resolve(garageId);

        GarageBrandingService.LogoContent logo = garageBrandingService.loadLogoOrNull(garageId);

        // New invoices print exactly what was persisted at issuance; legacy invoices keep the
        // original recomputation so their historical figures are unchanged.
        InvoiceSnapshotView snapshot = null;
        InvoiceTaxCalculator.Breakdown breakdown;

        if (invoice.isTaxSnapshot()) {
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoice.getId());
            snapshot = InvoiceSnapshotView.of(invoice, items);
            breakdown = InvoiceSnapshotView.breakdownOf(invoice, items);
        } else {
            breakdown = InvoiceTaxCalculator.calculate(
                    finalBillableLines(invoice, estimate), invoice.getDiscount());
        }

        Customer customer = jobCard.getCustomer();
        Vehicle vehicle = jobCard.getVehicle();

        InvoiceDocumentData data = new InvoiceDocumentData(
                garage,
                logo == null ? null : logo.bytes(),
                invoice.getInvoiceNumber(),
                invoiceDate(invoice),
                jobCard.getJobCardNumber(),
                vehicle.getRegistrationNumber(),
                // D11: persisted creator attribution only (advisor if the creator acted as one, otherwise the
                // responsible manager / owner, otherwise a neutral label). Never guessed.
                com.garageos.core.util.StaffAttribution.serviceAdvisorLabel(jobCard),
                customer.getFullName(),
                customer.getMobileNumber(),
                customerAddress(customer),
                join(" ", vehicle.getBrand(), vehicle.getModel(), vehicle.getVariant()),
                vehicleDetails(vehicle, jobCard),
                invoice.getRemarks(),
                invoice.getPaymentStatus() == null ? null : invoice.getPaymentStatus().name(),
                breakdown,
                snapshot,
                signatureBytes,
                stampBytes);

        return new InvoicePdf(
                renderer.render(data),
                "Invoice-" + invoice.getInvoiceNumber().replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf");
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceTaxCalculator.Breakdown breakdownFor(Invoice invoice) {
        if (invoice.isTaxSnapshot()) {
            return InvoiceSnapshotView.breakdownOf(
                    invoice, invoiceItemRepository.findByInvoiceId(invoice.getId()));
        }
        return InvoiceTaxCalculator.calculate(
                finalBillableLines(invoice, invoice.getEstimate()), invoice.getDiscount());
    }

    /**
     * Invoice Source of Truth. Only the FINAL billable state is ever
     * invoiced: the invoice's own line snapshot when it has one, otherwise
     * the estimate's items the customer kept selected. Items the customer
     * removed (selected = false) are never read back in, so they contribute
     * no line, no taxable value, no GST and nothing to the total.
     */
    List<InvoiceTaxCalculator.Line> finalBillableLines(Invoice invoice, Estimate estimate) {

        List<InvoiceTaxCalculator.Line> lines = new ArrayList<>();

        List<InvoiceItem> snapshot = invoiceItemRepository.findByInvoiceId(invoice.getId());

        if (!snapshot.isEmpty()) {
            for (InvoiceItem item : snapshot) {
                lines.add(new InvoiceTaxCalculator.Line(
                        item.getItemType(), item.getDescription(),
                        item.getQuantity(), item.getUnitPrice(), item.getTotalPrice()));
            }
            return lines;
        }

        for (EstimateItem item : estimateItemRepository.findByEstimateId(estimate.getId())) {

            if (!Boolean.TRUE.equals(item.getSelected())) {
                continue;
            }

            lines.add(new InvoiceTaxCalculator.Line(
                    item.getItemType(), item.getDescription(),
                    item.getQuantity(), item.getUnitPrice(), item.getTotalPrice()));
        }

        return lines;
    }

    /** Garage authorization assets; optional so hand-built unit-test instances keep the previous behaviour. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.modules.garage.service.GarageAuthorizationAssetService authorizationAssets;

    /**
     * LEGACY POLICY: invoices generated before this instant (ISO-8601 local date-time) keep their existing PDF,
     * which prints the blank "Authorised Signatory" line, so deploying the requirement does not break the
     * download of historical paid invoices. Empty (the default) = no exemption: every final invoice PDF requires
     * the garage signature and stamp. Set it to the deployment time to exempt only pre-existing invoices.
     */
    @org.springframework.beans.factory.annotation.Value("${garagest.invoice.authorization-cutover:}")
    private String authorizationCutover;

    private boolean legacyExempt(Invoice invoice) {

        if (authorizationCutover == null || authorizationCutover.isBlank() || invoice.getGeneratedAt() == null) {
            return false;
        }

        try {
            return invoice.getGeneratedAt().isBefore(java.time.LocalDateTime.parse(authorizationCutover.trim()));
        } catch (java.time.format.DateTimeParseException e) {
            return false; // a malformed value never silently exempts anything
        }
    }

    /**
     * The garage owner authorized signature and stamp are mandatory on a final invoice. Missing assets are an
     * actionable refusal - never a silently non-compliant invoice and never a fabricated signature. Staff of the
     * garage are told exactly what to upload; a customer is told the garage must finish its setup.
     */
    private void requireGarageAuthorization(GarageUserPrincipal principal, Long garageId, Invoice invoice) {

        if (authorizationAssets == null || legacyExempt(invoice)) {
            return;
        }

        java.util.List<com.garageos.modules.garage.service.GarageAuthorizationAssetService.Kind> missing =
                authorizationAssets.missing(garageId);

        if (missing.isEmpty()) {
            return;
        }

        boolean staff = com.garageos.core.security.GarageScope.isMemberOfGarage(garageScope, principal, garageId);

        String message = staff
                ? "The invoice cannot be issued until the garage owner uploads the authorized "
                  + missing.stream().map(k -> k.label()).collect(java.util.stream.Collectors.joining(" and "))
                  + ". The owner can add them under the garage settings."
                : "The garage has not finished its invoice authorization setup, so this invoice PDF cannot be issued yet. "
                  + "Please contact the garage.";

        throw new BusinessException(message, "GARAGE_AUTHORIZATION_ASSETS_MISSING");
    }

    /**
     * The final invoice PDF is issued only once the invoice is fully PAID, for every
     * caller (staff and customer alike). Invoice details and the amount due stay
     * viewable through the invoice read endpoints. Owner-authorized credit, delivery
     * status and partial payment never satisfy this condition.
     */
    static final String PAYMENT_PENDING_CODE = "INVOICE_PDF_PAYMENT_PENDING";

    private void requireFullyPaid(Invoice invoice) {

        if (invoice.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessException(
                    "Payment is pending for this invoice, so it cannot be printed or downloaded yet. "
                            + "The invoice PDF is available once the full amount has been paid.",
                    PAYMENT_PENDING_CODE);
        }
    }

    private void authorize(GarageUserPrincipal principal, JobCard jobCard) {

        if (jobCard.getGarage() != null
                && com.garageos.core.security.GarageScope.isMemberOfGarage(garageScope, principal, jobCard.getGarage().getId())) {
            return;
        }

        if (principal.getMobile() != null) {

            Customer customer = customerRepository
                    .findByMobileNumber(principal.getMobile())
                    .orElse(null);

            if (customer != null
                    && jobCard.getCustomer() != null
                    && jobCard.getCustomer().getId().equals(customer.getId())) {
                return;
            }
        }

        throw new ResourceNotFoundException("Invoice not found.");
    }

    private LocalDate invoiceDate(Invoice invoice) {

        if (invoice.getGeneratedAt() != null) {
            return invoice.getGeneratedAt().toLocalDate();
        }

        return invoice.getCreatedAt() == null ? null : invoice.getCreatedAt().toLocalDate();
    }

    private String customerAddress(Customer customer) {

        String cityLine = join(", ", customer.getCity(), customer.getState());

        if (customer.getPincode() != null && !customer.getPincode().isBlank()) {
            cityLine = cityLine + " - " + customer.getPincode();
        }

        return join(", ", customer.getAddress(), cityLine);
    }

    private String vehicleDetails(Vehicle vehicle, JobCard jobCard) {

        return join("  |  ",
                vehicle.getFuelType() == null ? null : vehicle.getFuelType().name(),
                vehicle.getManufacturingYear() == null ? null : "Year " + vehicle.getManufacturingYear(),
                vehicle.getColor(),
                jobCard.getOdometerReading() == null ? null : "Odometer " + jobCard.getOdometerReading() + " km");
    }

    private static String join(String separator, String... parts) {

        List<String> present = new ArrayList<>();

        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                present.add(part.trim());
            }
        }

        return String.join(separator, present);
    }
}
