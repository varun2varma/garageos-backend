package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.navigation.NavigationRequestStatus;
import com.garageos.core.enums.navigation.NavigationRequestType;
import com.garageos.modules.navigation.dto.request.CreateNavigationRequest;
import com.garageos.modules.navigation.dto.response.NavigationRequestResponse;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.service.NavigationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NavigationRequestServiceImpl
        implements NavigationRequestService {

    private final NavigationRequestRepository
            navigationRequestRepository;

    @Override
    @Transactional
    public NavigationRequestResponse createRequest(
            Long customerId,
            CreateNavigationRequest request) {

        validateRequest(request);

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

                        .pickupAddress(
                                request.getPickupAddress()
                        )

                        .deliveryAddress(
                                request.getDeliveryAddress()
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

                .pickupAddress(
                        request.getPickupAddress()
                )

                .deliveryAddress(
                        request.getDeliveryAddress()
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