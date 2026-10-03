package com.garageos.modules.invoice.service.impl;

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

        // JobCard -> garageId -> Garage -> branding. Never the viewer.
        Long garageId = jobCard.getGarage().getId();

        GarageBrandingResponse garage = garageBrandingService.resolve(garageId);

        GarageBrandingService.LogoContent logo = garageBrandingService.loadLogoOrNull(garageId);

        InvoiceTaxCalculator.Breakdown breakdown =
                InvoiceTaxCalculator.calculate(finalBillableLines(invoice, estimate), invoice.getDiscount());

        Customer customer = jobCard.getCustomer();
        Vehicle vehicle = jobCard.getVehicle();

        InvoiceDocumentData data = new InvoiceDocumentData(
                garage,
                logo == null ? null : logo.bytes(),
                invoice.getInvoiceNumber(),
                invoiceDate(invoice),
                jobCard.getJobCardNumber(),
                vehicle.getRegistrationNumber(),
                // The domain has no service-advisor assignment on a job card,
                // so none is printed rather than guessing one.
                null,
                customer.getFullName(),
                customer.getMobileNumber(),
                customerAddress(customer),
                join(" ", vehicle.getBrand(), vehicle.getModel(), vehicle.getVariant()),
                vehicleDetails(vehicle, jobCard),
                invoice.getRemarks(),
                breakdown);

        return new InvoicePdf(
                renderer.render(data),
                "Invoice-" + invoice.getInvoiceNumber().replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf");
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

    private void authorize(GarageUserPrincipal principal, JobCard jobCard) {

        if (principal.getGarageId() != null
                && jobCard.getGarage() != null
                && principal.getGarageId().equals(jobCard.getGarage().getId())) {
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
