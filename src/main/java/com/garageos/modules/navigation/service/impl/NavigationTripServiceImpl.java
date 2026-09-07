package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.navigation.*;
import com.garageos.modules.navigation.dto.request.CreateNavigationTripRequest;
import com.garageos.modules.navigation.dto.response.NavigationTripResponse;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import com.garageos.modules.navigation.service.NavigationTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NavigationTripServiceImpl
        implements NavigationTripService {

    private final NavigationRequestRepository
            navigationRequestRepository;

    private final NavigationTripRepository
            navigationTripRepository;

    private final NavigationTripMediaRepository
            navigationTripMediaRepository;


    @Override
    @Transactional
    public NavigationTripResponse assignDriver(
            CreateNavigationTripRequest request) {

        NavigationRequest navigationRequest =
                navigationRequestRepository
                        .findById(
                                request.getNavigationRequestId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Navigation request not found: "
                                                + request.getNavigationRequestId()
                                )
                        );


        if (navigationRequest.getStatus()
                != NavigationRequestStatus.REQUESTED) {

            throw new IllegalStateException(
                    "Navigation request is not available for assignment."
            );
        }


        if (request.getDriverId() == null) {

            throw new IllegalArgumentException(
                    "Driver ID is required."
            );
        }


        TripType tripType =
                navigationRequest.getRequestType()
                        == NavigationRequestType.PICKUP

                        ? TripType.PICKUP

                        : TripType.DELIVERY;


        /*
         * Initial movement:
         *
         * PICKUP:
         * Garage -> Customer
         *
         * DELIVERY:
         * Garage -> Customer
         */

        TripLeg initialLeg =
                TripLeg.GARAGE_TO_CUSTOMER;


        NavigationTrip trip =
                NavigationTrip.builder()

                        .navigationRequestId(
                                navigationRequest.getId()
                        )

                        .vehicleId(
                                navigationRequest.getVehicleId()
                        )

                        .driverId(
                                request.getDriverId()
                        )

                        .tripType(
                                tripType
                        )

                        .currentLeg(
                                initialLeg
                        )

                        .status(
                                TripStatus.ASSIGNED
                        )

                        .sourceAddress(
                                navigationRequest.getRequestType()
                                        == NavigationRequestType.PICKUP
                                        ? navigationRequest.getGarageId()
                                        .toString()
                                        : navigationRequest.getGarageId()
                                        .toString()
                        )

                        .destinationAddress(
                                navigationRequest.getRequestType()
                                        == NavigationRequestType.PICKUP
                                        ? navigationRequest.getPickupAddress()
                                        : navigationRequest.getDeliveryAddress()
                        )

                        .build();


        NavigationTrip savedTrip =
                navigationTripRepository.save(trip);


        navigationRequest.setStatus(
                NavigationRequestStatus.ASSIGNED
        );

        navigationRequestRepository.save(
                navigationRequest
        );


        return toResponse(savedTrip);
    }


    @Override
    @Transactional(readOnly = true)
    public NavigationTripResponse getTrip(
            Long tripId) {

        NavigationTrip trip =
                navigationTripRepository
                        .findById(tripId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Navigation trip not found: "
                                                + tripId
                                )
                        );

        return toResponse(trip);
    }


    @Override
    @Transactional(readOnly = true)
    public List<NavigationTripResponse> getDriverTrips(
            Long driverId) {

        return navigationTripRepository
                .findByDriverIdAndStatusIn(
                        driverId,
                        List.of(
                                TripStatus.ASSIGNED,
                                TripStatus.ACCEPTED,
                                TripStatus.IN_PROGRESS
                        )
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Override
    @Transactional
    public NavigationTripResponse acceptTrip(
            Long tripId,
            Long driverId) {

        NavigationTrip trip =
                getDriverTrip(
                        tripId,
                        driverId
                );


        if (trip.getStatus()
                != TripStatus.ASSIGNED) {

            throw new IllegalStateException(
                    "Trip cannot be accepted in current status."
            );
        }


        trip.setStatus(
                TripStatus.ACCEPTED
        );

        trip.setAcceptedAt(
                LocalDateTime.now()
        );


        return toResponse(
                navigationTripRepository.save(trip)
        );
    }


    @Override
    @Transactional
    public NavigationTripResponse startTrip(
            Long tripId,
            Long driverId) {

        NavigationTrip trip =
                getDriverTrip(
                        tripId,
                        driverId
                );


        if (trip.getStatus()
                != TripStatus.ACCEPTED) {

            throw new IllegalStateException(
                    "Trip must be accepted before starting."
            );
        }


        trip.setStatus(
                TripStatus.IN_PROGRESS
        );

        trip.setStartedAt(
                LocalDateTime.now()
        );


        return toResponse(
                navigationTripRepository.save(trip)
        );
    }


    @Override
    @Transactional
    public NavigationTripResponse arriveAtDestination(
            Long tripId,
            Long driverId) {

        NavigationTrip trip =
                getDriverTrip(
                        tripId,
                        driverId
                );


        if (trip.getStatus()
                != TripStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Trip is not in progress."
            );
        }


        trip.setArrivedAt(
                LocalDateTime.now()
        );


        /*
         * PICKUP:
         *
         * Driver has reached customer.
         *
         * We now wait for:
         * BEFORE_PICKUP photos
         *
         * and then the return leg.
         */

        if (trip.getTripType()
                == TripType.PICKUP
                && trip.getCurrentLeg()
                == TripLeg.GARAGE_TO_CUSTOMER) {

            return toResponse(
                    navigationTripRepository.save(trip)
            );
        }


        /*
         * DELIVERY:
         *
         * Driver has reached customer.
         *
         * Delivery photos are required before completion.
         */

        return toResponse(
                navigationTripRepository.save(trip)
        );
    }


    @Override
    @Transactional
    public NavigationTripResponse continueTrip(
            Long tripId,
            Long driverId) {

        NavigationTrip trip =
                getDriverTrip(
                        tripId,
                        driverId
                );


        if (trip.getTripType()
                != TripType.PICKUP) {

            throw new IllegalStateException(
                    "Only pickup trips can continue to the return leg."
            );
        }


        if (trip.getCurrentLeg()
                != TripLeg.GARAGE_TO_CUSTOMER) {

            throw new IllegalStateException(
                    "Pickup trip is already on the return leg."
            );
        }

        validateRequiredMedia(
                tripId,
                TripMediaStage.BEFORE_PICKUP
        );


        trip.setCurrentLeg(
                TripLeg.CUSTOMER_TO_GARAGE
        );


        trip.setSourceAddress(
                trip.getDestinationAddress()
        );


        /*
         * For now destination is the garage.
         *
         * We will replace this with the actual garage
         * address once the Garage entity/address contract
         * is connected.
         */


        trip.setDestinationAddress(
                "GARAGE"
        );


        trip.setArrivedAt(null);


        return toResponse(
                navigationTripRepository.save(trip)
        );
    }


    @Override
    @Transactional
    public NavigationTripResponse completeTrip(
            Long tripId,
            Long driverId) {

        NavigationTrip trip =
                getDriverTrip(
                        tripId,
                        driverId
                );


        if (trip.getStatus()
                != TripStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Trip is not in progress."
            );
        }


        if (trip.getTripType()
                == TripType.DELIVERY) {

            validateRequiredMedia(
                    tripId,
                    TripMediaStage.DELIVERY
            );
        }


        if (trip.getTripType()
                == TripType.PICKUP) {

            if (trip.getCurrentLeg()
                    != TripLeg.CUSTOMER_TO_GARAGE) {

                throw new IllegalStateException(
                        "Pickup trip must return to garage before completion."
                );
            }

            /*
             * Before-pickup evidence is mandatory
             * before the return leg.
             */
            validateRequiredMedia(
                    tripId,
                    TripMediaStage.BEFORE_PICKUP
            );
        }


        trip.setStatus(
                TripStatus.COMPLETED
        );

        trip.setCompletedAt(
                LocalDateTime.now()
        );


        return toResponse(
                navigationTripRepository.save(trip)
        );
    }

    private void validateRequiredMedia(
            Long tripId,
            TripMediaStage stage) {

        boolean exists =
                !navigationTripMediaRepository
                        .findByTripIdAndMediaStageOrderByCapturedAtAsc(
                                tripId,
                                stage
                        )
                        .isEmpty();


        if (!exists) {

            throw new IllegalStateException(
                    "Required "
                            + stage
                            + " photos are missing."
            );
        }
    }


    private NavigationTrip getDriverTrip(
            Long tripId,
            Long driverId) {

        return navigationTripRepository
                .findByIdAndDriverId(
                        tripId,
                        driverId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Trip not found for driver."
                        )
                );
    }


    private NavigationTripResponse toResponse(
            NavigationTrip trip) {

        return NavigationTripResponse.builder()

                .id(trip.getId())

                .navigationRequestId(
                        trip.getNavigationRequestId()
                )

                .vehicleId(
                        trip.getVehicleId()
                )

                .driverId(
                        trip.getDriverId()
                )

                .tripType(
                        trip.getTripType()
                )

                .currentLeg(
                        trip.getCurrentLeg()
                )

                .status(
                        trip.getStatus()
                )

                .sourceAddress(
                        trip.getSourceAddress()
                )

                .destinationAddress(
                        trip.getDestinationAddress()
                )

                .acceptedAt(
                        trip.getAcceptedAt()
                )

                .startedAt(
                        trip.getStartedAt()
                )

                .arrivedAt(
                        trip.getArrivedAt()
                )

                .completedAt(
                        trip.getCompletedAt()
                )

                .build();
    }
}