package com.garageos.modules.navigation.service.impl;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.navigation.*;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.handover.repository.VehicleHandoverRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.navigation.dto.request.CreateNavigationTripRequest;
import com.garageos.modules.navigation.dto.response.NavigationTripResponse;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripMediaRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import com.garageos.modules.navigation.service.NavigationTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private final VehicleHandoverRepository
            vehicleHandoverRepository;

    private final CustomerRepository
            customerRepository;

    private final GarageRepository
            garageRepository;

    private final UserRepository
            userRepository;


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


        /*
         * Corrective security fix: this had no authorization at all. Any
         * authenticated caller could assign any driver to any garage's
         * navigation request, simply by supplying the ids. Assignment is
         * a garage operation, so it is now scoped to the garage that owns
         * the request, and the driver must belong to that same garage.
         */
        requireOperationalStaffOfGarage(navigationRequest.getGarageId());

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


        requireDriverOfGarage(
                request.getDriverId(),
                navigationRequest.getGarageId()
        );


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

                        // Corrective fix: both branches of this ternary
                        // were identical and both wrote the garage *id*
                        // into an address field, so a driver's trip card
                        // showed a bare number like "10" as the place they
                        // were starting from. Both legs do start at the
                        // garage, so the ternary was pointless - what was
                        // missing was the garage's actual address.
                        .sourceAddress(
                                garageAddress(navigationRequest.getGarageId())
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
    public NavigationTripResponse getTripByRequest(Long navigationRequestId) {

        NavigationRequest navigationRequest = navigationRequestRepository
                .findById(navigationRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Navigation request not found: " + navigationRequestId));

        NavigationTrip trip = navigationTripRepository
                .findFirstByNavigationRequestIdOrderByIdDesc(navigationRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No trip has been created for this request yet."));

        authorizeViewer(trip, navigationRequest);

        return toResponse(trip);
    }

    /**
     * The request's own customer, its assigned driver, or garage-matched
     * operational staff only - mirrors
     * DriverLocationServiceImpl.authorizeViewer's three-way split.
     */
    private void authorizeViewer(NavigationTrip trip, NavigationRequest navigationRequest) {

        GarageUserPrincipal principal = (GarageUserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal.getRoles().contains(RoleCode.CUSTOMER.name())) {

            Customer customer = customerRepository.findByMobileNumber(principal.getMobile())
                    .orElseThrow(() -> new ResourceNotFoundException("Trip not found."));

            if (!navigationRequest.getCustomerId().equals(customer.getId())) {
                throw new ResourceNotFoundException("Trip not found.");
            }

            return;
        }

        boolean isAssignedDriver = trip.getDriverId() != null
                && trip.getDriverId().equals(principal.getId());

        boolean isSameGarageEmployee = principal.getGarageId() != null
                && principal.getGarageId().equals(navigationRequest.getGarageId());

        if (!isAssignedDriver && !isSameGarageEmployee) {
            throw new ResourceNotFoundException("Trip not found.");
        }
    }

    /**
     * A driver's own open work queue. Same corrective fix as
     * {@link #getDriverTrip}: this endpoint previously returned any
     * driver's trips to any authenticated caller who guessed the id.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NavigationTripResponse> getDriverTrips(
            Long driverId) {

        requireCallerIsDriver(driverId);

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

    /**
     * A driver's own finished work - COMPLETED and CANCELLED trips, most
     * recent first. Added because the driver persona had no truthful way
     * to see past trips at all; capped rather than unbounded so a long
     * serving driver's history cannot pull an unbounded result set into
     * the app.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NavigationTripResponse> getDriverTripHistory(
            Long driverId,
            int limit) {

        requireCallerIsDriver(driverId);

        int cappedLimit = Math.min(Math.max(limit, 1), 100);

        return navigationTripRepository
                .findByDriverIdAndStatusInOrderByIdDesc(
                        driverId,
                        List.of(
                                TripStatus.COMPLETED,
                                TripStatus.CANCELLED
                        ),
                        PageRequest.of(0, cappedLimit)
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

        requireVerifiedHandover(tripId, TripType.PICKUP);

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

            requireVerifiedHandover(tripId, TripType.DELIVERY);
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

            // The PICKUP handover was already required to reach the
            // CUSTOMER_TO_GARAGE leg in continueTrip() - re-checked here
            // too rather than trusting that as an invariant, since it's a
            // cheap read and this method is the last gate before COMPLETED.
            requireVerifiedHandover(tripId, TripType.PICKUP);
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

    /**
     * The real trip gate: not "does a handover row exist" but "is there a
     * VERIFIED handover for exactly this trip and this direction." A
     * PICKUP handover can never authorize a DELIVERY completion (or vice
     * versa), and one trip's handover can never authorize another trip's -
     * both enforced by the repository query itself, not re-derived here.
     */
    private void requireVerifiedHandover(
            Long tripId,
            TripType direction) {

        boolean verified =
                vehicleHandoverRepository.existsByTripIdAndDirectionAndStatus(
                        tripId,
                        direction,
                        HandoverStatus.VERIFIED
                );

        if (!verified) {

            throw new IllegalStateException(
                    "Vehicle handover has not been verified yet - "
                            + "ask the customer for the confirmation code."
            );
        }
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


    /**
     * Corrective security fix: this previously validated only that the
     * trip belonged to {@code driverId}, never that the *caller* was
     * that driver. Because every driver-side transition endpoint takes
     * driverId as a plain request parameter, any authenticated user could
     * pass another driver's id and accept/start/arrive/continue/complete
     * that driver's trip. The caller's own identity is now the authority;
     * driverId is only honoured when it matches the principal.
     */
    private NavigationTrip getDriverTrip(
            Long tripId,
            Long driverId) {

        requireCallerIsDriver(driverId);

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

    /**
     * A driver may only ever act as themselves. Deliberately throws the
     * same not-found style error the repository lookup would, so probing
     * another driver's id cannot be used to distinguish "exists but not
     * yours" from "does not exist".
     */
    private void requireCallerIsDriver(Long driverId) {

        GarageUserPrincipal principal = currentPrincipal();

        if (driverId == null
                || principal.getId() == null
                || !principal.getId().equals(driverId)) {

            throw new ResourceNotFoundException(
                    "Trip not found for driver."
            );
        }
    }

    /**
     * Assigning a driver is a garage operation. The caller must be
     * operational staff of the garage that owns the navigation request,
     * and the driver they nominate must belong to that same garage - so a
     * manager cannot dispatch another garage's driver, and cannot touch
     * another garage's pickup at all.
     *
     * Deliberately throws not-found rather than forbidden, matching the
     * rest of this service, so probing ids cannot be used to discover
     * that a request exists in another garage.
     */
    private void requireOperationalStaffOfGarage(Long garageId) {

        GarageUserPrincipal principal = currentPrincipal();

        if (garageId == null
                || principal.getGarageId() == null
                || !principal.getGarageId().equals(garageId)) {

            throw new ResourceNotFoundException(
                    "Navigation request not found."
            );
        }
    }

    private void requireDriverOfGarage(Long driverId, Long garageId) {

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Driver not found."));

        if (driver.getGarageId() == null
                || !driver.getGarageId().equals(garageId)) {

            throw new BusinessException(
                    "That driver does not belong to this garage."
            );
        }
    }

    /**
     * A readable starting address for the trip: the garage's own address,
     * falling back to its name and then its code. Never the raw id.
     */
    private String garageAddress(Long garageId) {

        if (garageId == null) {
            return null;
        }

        return garageRepository.findById(garageId)
                .map(garage -> {

                    if (garage.getAddress() != null
                            && !garage.getAddress().isBlank()) {
                        return garage.getAddress();
                    }

                    if (garage.getGarageName() != null
                            && !garage.getGarageName().isBlank()) {
                        return garage.getGarageName();
                    }

                    return garage.getGarageCode();
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
     * Enriches the trip with the canonical destination coordinates and
     * the customer id, both read from the NavigationRequest this trip was
     * created for.
     *
     * Resolved rather than copied onto the trip: the request already owns
     * the location, and duplicating it would create two places for it to
     * drift. A driver's trip lists are their own and capped, so the extra
     * lookup is bounded.
     *
     * A request that cannot be found leaves the coordinates null - the
     * trip is still returned, because a driver losing their whole queue
     * over one missing row would be worse than a trip with no map point.
     */
    private NavigationTripResponse toResponse(
            NavigationTrip trip) {

        NavigationRequest request =
                trip.getNavigationRequestId() == null
                        ? null
                        : navigationRequestRepository
                                .findById(trip.getNavigationRequestId())
                                .orElse(null);

        BigDecimal destinationLatitude = null;
        BigDecimal destinationLongitude = null;
        Long customerId = null;

        if (request != null) {

            customerId = request.getCustomerId();

            boolean pickup = trip.getTripType() == TripType.PICKUP;

            destinationLatitude = pickup
                    ? request.getPickupLatitude()
                    : request.getDeliveryLatitude();

            destinationLongitude = pickup
                    ? request.getPickupLongitude()
                    : request.getDeliveryLongitude();
        }

        return NavigationTripResponse.builder()

                .destinationLatitude(destinationLatitude)

                .destinationLongitude(destinationLongitude)

                .customerId(customerId)

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