package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.navigation.NavigationRequestStatus;
import com.garageos.core.enums.navigation.NavigationRequestType;
import com.garageos.modules.navigation.dto.request.CreateNavigationRequest;
import com.garageos.modules.navigation.dto.response.NavigationRequestResponse;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.service.NavigationRequestService;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NavigationRequestServiceImpl
        implements NavigationRequestService {

    private final NavigationRequestRepository
            navigationRequestRepository;

    private final JobCardRepository jobCardRepository;

    @Override
    @Transactional
    public NavigationRequestResponse createRequest(
            Long customerId,
            CreateNavigationRequest request) {

        validateRequest(request);

        Long jobCardId = null;

        if (request.getRequestType() == NavigationRequestType.DELIVERY) {
            jobCardId = validateDeliveryRequest(customerId, request);
        }

        NavigationRequest navigationRequest =
                NavigationRequest.builder()

                        .customerId(customerId)

                        .vehicleId(
                                request.getVehicleId()
                        )

                        .garageId(
                                request.getGarageId()
                        )

                        .requestType(
                                request.getRequestType()
                        )
                        .jobCardId(jobCardId)

                        .pickupAddress(
                                request.getPickupAddress()
                        )

                        .pickupLatitude(
                                request.getPickupLatitude()
                        )

                        .pickupLongitude(
                                request.getPickupLongitude()
                        )

                        .deliveryAddress(
                                request.getDeliveryAddress()
                        )

                        .deliveryLatitude(
                                request.getDeliveryLatitude()
                        )

                        .deliveryLongitude(
                                request.getDeliveryLongitude()
                        )

                        .scheduledAt(
                                request.getScheduledAt()
                        )

                        .status(
                                NavigationRequestStatus.REQUESTED
                        )

                        .build();

        NavigationRequest saved =
                navigationRequestRepository.save(
                        navigationRequest
                );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NavigationRequestResponse getRequest(
            Long requestId) {

        NavigationRequest request =
                navigationRequestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Navigation request not found: "
                                                + requestId
                                )
                        );

        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NavigationRequestResponse> getCustomerRequests(
            Long customerId) {

        return navigationRequestRepository
                .findByCustomerIdOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NavigationRequestResponse> getGarageRequests(
            Long garageId) {

        return navigationRequestRepository
                .findByGarageIdAndStatusOrderByScheduledAtAsc(
                        garageId,
                        NavigationRequestStatus.REQUESTED
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NavigationRequestResponse toResponse(
            NavigationRequest request) {

        return NavigationRequestResponse.builder()

                .id(request.getId())

                .customerId(
                        request.getCustomerId()
                )

                .vehicleId(
                        request.getVehicleId()
                )

                .garageId(
                        request.getGarageId()
                )

                .requestType(
                        request.getRequestType()
                )
                .jobCardId(request.getJobCardId())

                .pickupAddress(
                        request.getPickupAddress()
                )

                .pickupLatitude(
                        request.getPickupLatitude()
                )

                .pickupLongitude(
                        request.getPickupLongitude()
                )

                .deliveryAddress(
                        request.getDeliveryAddress()
                )

                .deliveryLatitude(
                        request.getDeliveryLatitude()
                )

                .deliveryLongitude(
                        request.getDeliveryLongitude()
                )

                .scheduledAt(
                        request.getScheduledAt()
                )

                .status(
                        request.getStatus()
                )

                .createdAt(
                        request.getCreatedAt()
                )

                .build();
    }

    /**
     * A DELIVERY request is the start of the delivery lifecycle for one
     * specific JobCard: it must reference a READY_FOR_DELIVERY JobCard of the
     * caller's own garage, for that JobCard's own customer/vehicle, carry the
     * chosen destination (address + coordinates), and be the only live
     * delivery request for that JobCard. Returns the validated JobCard id.
     */
    private Long validateDeliveryRequest(
            Long customerId,
            CreateNavigationRequest request) {

        if (request.getJobCardId() == null) {
            throw new BusinessException(
                    "A delivery request must reference the Job Card being delivered.");
        }

        if (request.getDeliveryLatitude() == null
                || request.getDeliveryLongitude() == null
                || isBlank(request.getDeliveryAddress())) {
            throw new BusinessException(
                    "A delivery request requires a delivery address with coordinates.");
        }

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job Card not found with id : " + request.getJobCardId()));

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext().getAuthentication().getPrincipal();

        if (jobCard.getGarage() == null
                || principal.getGarageId() == null
                || !principal.getGarageId().equals(jobCard.getGarage().getId())
                || !jobCard.getGarage().getId().equals(request.getGarageId())) {
            throw new BusinessException(
                    "This Job Card does not belong to your garage.");
        }

        if (jobCard.getStatus() != JobCardStatus.READY_FOR_DELIVERY) {
            throw new BusinessException(
                    "Delivery can only be requested once the Job Card is ready for delivery.");
        }

        if (jobCard.getVehicle() == null
                || !jobCard.getVehicle().getId().equals(request.getVehicleId())
                || jobCard.getCustomer() == null
                || !jobCard.getCustomer().getId().equals(customerId)) {
            throw new BusinessException(
                    "The delivery request does not match the Job Card's vehicle and customer.");
        }

        if (navigationRequestRepository.existsByJobCardIdAndRequestTypeAndStatusNot(
                jobCard.getId(),
                NavigationRequestType.DELIVERY,
                NavigationRequestStatus.CANCELLED)) {
            throw new BusinessException(
                    "A delivery has already been requested for this Job Card.");
        }

        return jobCard.getId();
    }

    /** The JobCard's delivery request (latest), for rediscovery when the Delivery step is reopened. */
    @Override
    @Transactional(readOnly = true)
    public NavigationRequestResponse getDeliveryRequestForJobCard(Long jobCardId) {

        JobCard jobCard = jobCardRepository.findById(jobCardId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job Card not found with id : " + jobCardId));

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext().getAuthentication().getPrincipal();

        if (jobCard.getGarage() == null
                || principal.getGarageId() == null
                || !principal.getGarageId().equals(jobCard.getGarage().getId())) {
            throw new ResourceNotFoundException(
                    "Job Card not found with id : " + jobCardId);
        }

        return navigationRequestRepository
                .findFirstByJobCardIdAndRequestTypeOrderByIdDesc(
                        jobCardId, NavigationRequestType.DELIVERY)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No delivery request exists for this Job Card."));
    }

    private void validateRequest(
            CreateNavigationRequest request) {

        if (request.getRequestType() == null) {

            throw new IllegalArgumentException(
                    "Request type is required"
            );
        }

        if (request.getVehicleId() == null) {

            throw new IllegalArgumentException(
                    "Vehicle ID is required"
            );
        }

        if (request.getGarageId() == null) {

            throw new IllegalArgumentException(
                    "Garage ID is required"
            );
        }

        if (request.getScheduledAt() == null) {

            throw new IllegalArgumentException(
                    "Scheduled time is required"
            );
        }

        /*
         * PICKUP:
         *
         * Customer -> Garage
         *
         * The customer's pickup address is required.
         */

        if (request.getRequestType() == NavigationRequestType.PICKUP
                && isBlank(request.getPickupAddress())) {

            throw new IllegalArgumentException(
                    "Pickup address is required"
            );
        }

        /*
         * DELIVERY:
         *
         * Garage -> Customer
         *
         * The delivery address is required.
         */

        if (request.getRequestType() == NavigationRequestType.DELIVERY
                && isBlank(request.getDeliveryAddress())) {

            throw new IllegalArgumentException(
                    "Delivery address is required"
            );
        }
    }

    private boolean isBlank(String value) {

        return value == null
                || value.trim().isEmpty();
    }
}