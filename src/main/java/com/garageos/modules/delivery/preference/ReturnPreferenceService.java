package com.garageos.modules.delivery.preference;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.navigation.NavigationRequestStatus;
import com.garageos.core.enums.navigation.NavigationRequestType;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.delivery.repository.DeliveryRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * The customer choice of how the vehicle comes back. Reuses the existing NavigationRequest (DELIVERY) for driver
 * delivery instead of adding a second booking/dispatch subsystem. The choice is only changeable before dispatch
 * (request still REQUESTED); afterwards the garage must cancel the trip through the existing flow.
 */
@Service
@RequiredArgsConstructor
public class ReturnPreferenceService {

    /** The single release rule; optional so hand-built test instances fall back to PAID-only. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.garageos.modules.credit.service.ReleaseClearance releaseClearance;

    private final ReturnPreferenceRepository preferenceRepository;
    private final JobCardRepository jobCardRepository;
    private final CustomerRepository customerRepository;
    private final NavigationRequestRepository navigationRequestRepository;
    private final NavigationTripRepository navigationTripRepository;
    private final DeliveryRepository deliveryRepository;
    private final com.garageos.modules.invoice.repository.InvoiceRepository invoiceRepository;
    private final GarageScope garageScope;

    /** Read for the owning customer or for staff of the job card garage; everyone else gets not-found. */
    @Transactional(readOnly = true)
    public ReturnPreferenceResponse get(Long jobCardId) {

        JobCard jobCard = requireReadable(jobCardId);

        return toResponse(jobCard, preferenceRepository.findByJobCardId(jobCardId).orElse(null));
    }

    /** Customer apps address a job card by its number; resolves to the same checks as {@link #get(Long)}. */
    @Transactional(readOnly = true)
    public ReturnPreferenceResponse getByNumber(String jobCardNumber) {
        return get(idForNumber(jobCardNumber));
    }

    @Transactional
    public ReturnPreferenceResponse setByNumber(String jobCardNumber, SetReturnPreferenceRequest request) {
        return set(idForNumber(jobCardNumber), request);
    }

    private Long idForNumber(String jobCardNumber) {
        return jobCardRepository.findByJobCardNumber(jobCardNumber)
                .map(JobCard::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Card not found : " + jobCardNumber));
    }

    @Transactional
    public ReturnPreferenceResponse set(Long jobCardId, SetReturnPreferenceRequest request) {

        GarageUserPrincipal principal = garageScope.principal();
        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Card not found with id : " + jobCardId));

        Customer customer = requireOwningCustomer(jobCard, principal);

        if (jobCard.getStatus() != JobCardStatus.READY_FOR_DELIVERY) {
            throw new BusinessException(
                    "You can choose how to get your vehicle back once it is ready for delivery.");
        }

        if (deliveryRepository.existsByJobCardId(jobCardId)) {
            throw new BusinessException("This vehicle has already been handed over.");
        }

        // Payment is confirmed by the backend, never inferred from the job status alone.
        if (!isPaid(jobCardId)) {
            throw new BusinessException("You can choose how to get your vehicle back once your payment is complete.");
        }

        NavigationRequest delivery = navigationRequestRepository
                .findFirstByJobCardIdAndRequestTypeOrderByIdDesc(jobCardId, NavigationRequestType.DELIVERY)
                .filter(r -> r.getStatus() != NavigationRequestStatus.CANCELLED)
                .orElse(null);

        if (delivery != null && delivery.getStatus() != NavigationRequestStatus.REQUESTED) {
            throw new BusinessException(
                    "A driver is already assigned to your delivery, so it can no longer be changed here. "
                            + "Please contact the garage.");
        }

        ReturnPreference preference = preferenceRepository.findByJobCardId(jobCardId).orElse(null);
        if (preference == null) {
            preference = new ReturnPreference();
            preference.setJobCardId(jobCardId);
            preference.setCustomerId(customer.getId());
        }

        if (request.getMethod() == ReturnMethod.COLLECT_AT_GARAGE) {

            if (delivery != null) {
                // Not yet dispatched: withdraw the open delivery request so no driver trip is created.
                delivery.setStatus(NavigationRequestStatus.CANCELLED);
                navigationRequestRepository.save(delivery);
            }

            preference.setMethod(ReturnMethod.COLLECT_AT_GARAGE);
            preference.setDeliveryAddress(null);
            preference.setDeliveryLatitude(null);
            preference.setDeliveryLongitude(null);

        } else {

            String address = request.getDeliveryAddress() == null ? "" : request.getDeliveryAddress().trim();
            validateDestination(address, request.getDeliveryLatitude(), request.getDeliveryLongitude());

            if (delivery == null) {
                delivery = navigationRequestRepository.save(NavigationRequest.builder()
                        .customerId(customer.getId())
                        .vehicleId(jobCard.getVehicle().getId())
                        .garageId(jobCard.getGarage().getId())
                        .requestType(NavigationRequestType.DELIVERY)
                        .jobCardId(jobCardId)
                        .deliveryAddress(address)
                        .deliveryLatitude(request.getDeliveryLatitude())
                        .deliveryLongitude(request.getDeliveryLongitude())
                        .status(NavigationRequestStatus.REQUESTED)
                        .build());
            } else {
                // Same customer re-submitting before dispatch: idempotent, address edits allowed.
                delivery.setDeliveryAddress(address);
                delivery.setDeliveryLatitude(request.getDeliveryLatitude());
                delivery.setDeliveryLongitude(request.getDeliveryLongitude());
                navigationRequestRepository.save(delivery);
            }

            preference.setMethod(ReturnMethod.DRIVER_DELIVERY);
            preference.setDeliveryAddress(address.length() > 500 ? address.substring(0, 500) : address);
            preference.setDeliveryLatitude(request.getDeliveryLatitude());
            preference.setDeliveryLongitude(request.getDeliveryLongitude());
        }

        try {
            preference = preferenceRepository.saveAndFlush(preference);
        } catch (DataIntegrityViolationException raced) {
            // Two simultaneous first submissions: the unique job_card_id constraint rejects the second.
            throw new BusinessException("Your choice is already being saved. Please refresh.");
        }

        return toResponse(jobCard, preference);
    }

    static void validateDestination(String address, BigDecimal lat, BigDecimal lon) {

        if (address.isEmpty() || lat == null || lon == null) {
            throw new BusinessException("Choose a delivery address on the map.");
        }
        if (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0
                || lon.compareTo(BigDecimal.valueOf(-180)) < 0 || lon.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BusinessException("The delivery location is not valid.");
        }
    }

    /** Release clearance (fully paid OR active Owner credit); the method name is kept for its existing callers. */
    private boolean isPaid(Long jobCardId) {
        return invoiceRepository.findByEstimateJobCardId(jobCardId)
                .map(i -> releaseClearance != null
                        ? releaseClearance.isCleared(i)
                        : i.getPaymentStatus() == com.garageos.core.enums.PaymentStatus.PAID)
                .orElse(false);
    }

    /** Presentation state derived from persisted data; no competing job-card status is stored. */
    static String deliveryState(boolean handedOver, boolean ready, ReturnPreference preference, NavigationTrip trip) {

        if (handedOver) {
            return "HANDED_OVER";
        }
        if (!ready) {
            return "NOT_READY";
        }
        if (preference == null || preference.getMethod() == null) {
            return "WAITING_FOR_CUSTOMER_PREFERENCE";
        }
        if (preference.getMethod() == ReturnMethod.COLLECT_AT_GARAGE) {
            return "COLLECTION_SELECTED";
        }
        if (trip == null) {
            return "DELIVERY_REQUESTED";
        }
        return switch (trip.getStatus()) {
            case ASSIGNED -> "DRIVER_ASSIGNED";
            case ACCEPTED -> "DRIVER_ACCEPTED";
            case IN_PROGRESS -> trip.getArrivedAt() != null ? "DRIVER_ARRIVED" : "IN_TRANSIT";
            case COMPLETED -> "HANDED_OVER";
            case CANCELLED -> "DELIVERY_REQUESTED";
        };
    }

    private ReturnPreferenceResponse toResponse(JobCard jobCard, ReturnPreference preference) {

        NavigationRequest delivery = navigationRequestRepository
                .findFirstByJobCardIdAndRequestTypeOrderByIdDesc(jobCard.getId(), NavigationRequestType.DELIVERY)
                .filter(r -> r.getStatus() != NavigationRequestStatus.CANCELLED)
                .orElse(null);

        NavigationTrip trip = delivery == null ? null
                : navigationTripRepository.findFirstByNavigationRequestIdOrderByIdDesc(delivery.getId()).orElse(null);

        boolean locked = delivery != null && delivery.getStatus() != NavigationRequestStatus.REQUESTED;
        boolean handedOver = deliveryRepository.existsByJobCardId(jobCard.getId());
        boolean paid = isPaid(jobCard.getId());
        boolean ready = jobCard.getStatus() == JobCardStatus.READY_FOR_DELIVERY && paid;

        return ReturnPreferenceResponse.builder()
                .jobCardId(jobCard.getId())
                .vehicleId(jobCard.getVehicle() == null ? null : jobCard.getVehicle().getId())
                .method(preference == null ? null : preference.getMethod())
                .deliveryAddress(preference == null ? null : preference.getDeliveryAddress())
                .deliveryLatitude(preference == null ? null : preference.getDeliveryLatitude())
                .deliveryLongitude(preference == null ? null : preference.getDeliveryLongitude())
                .selectedAt(preference == null ? null : preference.getUpdatedAt())
                .navigationRequestId(delivery == null ? null : delivery.getId())
                .navigationRequestStatus(delivery == null ? null : delivery.getStatus().name())
                .tripId(trip == null ? null : trip.getId())
                .tripStatus(trip == null ? null : trip.getStatus().name())
                .locked(locked)
                .canChoose(!locked && !handedOver && ready)
                .deliveryState(deliveryState(handedOver, ready, preference, trip))
                .build();
    }

    private JobCard requireReadable(Long jobCardId) {

        GarageUserPrincipal principal = garageScope.principal();
        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Card not found with id : " + jobCardId));

        if (garageScope.isCustomerOnly(principal)) {
            requireOwningCustomer(jobCard, principal);
            return jobCard;
        }

        if (jobCard.getGarage() == null
                || !GarageScope.isMemberOfGarage(garageScope, principal, jobCard.getGarage().getId())) {
            throw new ResourceNotFoundException("Job Card not found with id : " + jobCardId);
        }
        return jobCard;
    }

    private Customer requireOwningCustomer(JobCard jobCard, GarageUserPrincipal principal) {

        if (principal == null || principal.getMobile() == null
                || !principal.getRoles().contains(RoleCode.CUSTOMER.name())) {
            throw new ResourceNotFoundException("Job Card not found with id : " + jobCard.getId());
        }

        Customer customer = customerRepository.findByMobileNumber(principal.getMobile())
                .orElseThrow(() -> new ResourceNotFoundException("Job Card not found with id : " + jobCard.getId()));

        if (jobCard.getCustomer() == null || !jobCard.getCustomer().getId().equals(customer.getId())
                || jobCard.getVehicle() == null || jobCard.getGarage() == null) {
            throw new ResourceNotFoundException("Job Card not found with id : " + jobCard.getId());
        }
        return customer;
    }
}
