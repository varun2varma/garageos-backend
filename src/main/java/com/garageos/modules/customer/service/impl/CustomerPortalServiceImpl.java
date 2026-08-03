package com.garageos.modules.customer.service.impl;

import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.dto.response.portal.*;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.mapper.CustomerPortalMapper;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.customer.service.CustomerPortalService;
import com.garageos.modules.estimate.dto.response.EstimateResponse;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.estimateitem.dto.response.EstimateItemResponse;
import com.garageos.modules.estimateitem.service.EstimateItemService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerPortalServiceImpl
        implements CustomerPortalService {

    private final CustomerRepository customerRepository;

    private final VehicleRepository vehicleRepository;

    private final JobCardRepository jobCardRepository;

    private final EstimateRepository estimateRepository;

    private final InvoiceRepository invoiceRepository;

    private final CustomerPortalMapper mapper;

    private final EstimateService estimateService;

    private final EstimateItemService estimateItemService;


    @Override
    public CustomerProfileResponse getProfile() {

        Customer customer = getCurrentCustomer();

        return mapper.toProfile(getCurrentCustomer());

    }

    private Customer getCurrentCustomer() {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return customerRepository
                .findByMobileNumber(principal.getMobile())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Customer not found."));
    }

    @Override
    public List<CustomerVehicleResponse> getVehicles() {

        Customer customer = getCurrentCustomer();

        return vehicleRepository
                .findByCustomer(customer)
                .stream()
                .map(mapper::toVehicle)
                .toList();

    }

    @Override
    public List<CustomerJobCardResponse> getJobCards() {

        Customer customer = getCurrentCustomer();

        return jobCardRepository
                .findByCustomer(getCurrentCustomer())
                .stream()
                .map(mapper::toJobCard)
                .toList();

    }

    @Override
    public List<CustomerEstimateResponse> getEstimates() {

        Customer customer = getCurrentCustomer();

        return estimateRepository
                .findByJobCardCustomer(customer)
                .stream()
                .map(mapper::toEstimate)
                .toList();

    }


    @Override
    public List<CustomerInvoiceResponse> getInvoices() {

        Customer customer = getCurrentCustomer();

        return invoiceRepository
                .findByEstimateJobCardCustomer(customer)
                .stream()
                .map(mapper::toInvoice)
                .toList();

    }

    @Override
    public CustomerDashboardResponse getDashboard() {

        Customer customer = getCurrentCustomer();

        return CustomerDashboardResponse.builder()
                .vehicleCount(
                        vehicleRepository.countByCustomer(customer))
                .activeJobCount(
                        jobCardRepository.countByCustomer(customer))
                .pendingEstimateCount(
                        estimateRepository.countByJobCardCustomer(customer))
                .pendingInvoiceCount(
                        invoiceRepository.countByEstimateJobCardCustomer(customer))
                .build();


    }

    @Override
    public CustomerRepairTrackingResponse trackRepair(String jobCardNumber) {

        Customer customer = getCurrentCustomer();

        JobCard jobCard =
                jobCardRepository.findByJobCardNumber(jobCardNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Job Card not found."));

        if (!jobCard.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Job Card not found.");
        }

        boolean estimatePrepared =
                estimateRepository.findByJobCardId(jobCard.getId()).isPresent();

        boolean invoiceGenerated =
                invoiceRepository.findByEstimateJobCardId(jobCard.getId()).isPresent();

        return CustomerRepairTrackingResponse.builder()
                .jobCardNumber(jobCard.getJobCardNumber())
                .registrationNumber(jobCard.getVehicle().getRegistrationNumber())
                .status(jobCard.getStatus())
                .serviceDate(jobCard.getServiceDate())
                .estimatedDeliveryDate(jobCard.getEstimatedDeliveryDate())

                .inspectionCompleted(true)
                .estimatePrepared(estimatePrepared)
                .estimateApproved(estimatePrepared)
                .repairCompleted(false)
                .qualityChecked(false)
                .invoiceGenerated(invoiceGenerated)
                .paymentCompleted(false)

                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerEstimateDetailsResponse getEstimateDetails(
            Long estimateId) {

        EstimateResponse estimate =
                estimateService.getEstimate(
                        estimateId
                );

        List<EstimateItemResponse> items =
                estimateItemService.getItems(
                        estimateId
                );

        return CustomerEstimateDetailsResponse.builder()
                .estimate(estimate)
                .items(items)
                .build();

    }



}