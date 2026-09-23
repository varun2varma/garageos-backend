package com.garageos.modules.customer.service.impl;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.booking.BookingStatus;
import com.garageos.core.enums.navigation.HandoverStatus;
import com.garageos.core.enums.navigation.NavigationRequestStatus;
import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.booking.entity.Booking;
import com.garageos.modules.booking.repository.BookingRepository;
import com.garageos.modules.customer.dto.response.portal.CustomerLiveVehicleJourneySummary;
import com.garageos.modules.customer.dto.response.portal.LiveVehicleJourneyStage;
import com.garageos.modules.customer.dto.response.portal.PrimaryActionType;
import com.garageos.modules.customer.dto.response.portal.VehicleJourneyBookingSummary;
import com.garageos.modules.customer.dto.response.portal.VehicleJourneyTripSummary;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.mapper.CustomerPortalMapper;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.customer.service.CustomerVehicleJourneyService;
import com.garageos.modules.handover.repository.VehicleHandoverRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import com.garageos.modules.vehicle.entity.Vehicle;
import com.garageos.modules.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Derives, at request time, one "what is happening with my vehicle right
 * now" summary per vehicle that currently has a relevant journey - the
 * backend for GET /api/v1/customer/vehicle-journeys/active.
 *
 * This is a pure read-only aggregation layer over Booking,
 * NavigationRequest, NavigationTrip and JobCard. It never writes to any
 * of them, never introduces a persisted "journey status", and never
 * changes RepairTask/JobCard/NavigationTrip transition logic - each of
 * those stays exactly as implemented in its own module. See
 * {@link #deriveStage} for the priority rule used when more than one
 * domain has a bearing on the same vehicle at once.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerVehicleJourneyServiceImpl
        implements CustomerVehicleJourneyService {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final NavigationRequestRepository navigationRequestRepository;
    private final NavigationTripRepository navigationTripRepository;
    private final JobCardRepository jobCardRepository;
    private final VehicleHandoverRepository vehicleHandoverRepository;
    private final CustomerPortalMapper mapper;

    /**
     * Booking.status values worth surfacing. REQUESTED is deliberately
     * excluded: nothing is actually happening with the vehicle yet until
     * the garage confirms it, so a bare service request is not "a
     * journey" for this card. REJECTED/CANCELLED/COMPLETED are terminal.
     */
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES =
            List.of(BookingStatus.CONFIRMED);

    /**
     * The only NavigationRequestStatus worth querying directly: REQUESTED
     * means "pickup confirmed, no driver assigned yet". Anything past
     * that always has a NavigationTrip, whose own TripStatus is what
     * actually reflects progress - NavigationRequestStatus stays at
     * ASSIGNED forever afterward and is never advanced to
     * IN_PROGRESS/COMPLETED anywhere in NavigationTripServiceImpl, so it
     * cannot be trusted as a "still active" signal past this point.
     */
    private static final List<NavigationRequestStatus> PENDING_ASSIGNMENT_STATUSES =
            List.of(NavigationRequestStatus.REQUESTED);

    private static final List<TripStatus> ACTIVE_TRIP_STATUSES =
            List.of(TripStatus.ASSIGNED, TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);

    /**
     * Mirrors JobCardStatusValidator's own terminal set. DELIVERED is
     * deliberately NOT terminal here so the customer still sees a final
     * "service completed" card once, before staff explicitly CLOSE the
     * job card.
     */
    private static final List<JobCardStatus> TERMINAL_JOB_CARD_STATUSES =
            List.of(JobCardStatus.CLOSED, JobCardStatus.CANCELLED);

    @Override
    public List<CustomerLiveVehicleJourneySummary> getActiveJourneys() {

        Customer customer = getCurrentCustomer();

        List<Vehicle> vehicles = vehicleRepository.findByCustomer(customer);

        if (vehicles.isEmpty()) {
            return List.of();
        }

        List<Long> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();

        // One batched query per domain across every vehicle this customer
        // owns, instead of one query per vehicle per domain (N+1).
        Map<Long, Booking> activeBookingByVehicle = latestByVehicle(
                bookingRepository.findByVehicleIdInAndStatusIn(vehicleIds, ACTIVE_BOOKING_STATUSES),
                Booking::getVehicleId,
                Booking::getId
        );

        Map<Long, NavigationRequest> pendingRequestByVehicle = latestByVehicle(
                navigationRequestRepository.findByVehicleIdInAndStatusIn(vehicleIds, PENDING_ASSIGNMENT_STATUSES),
                NavigationRequest::getVehicleId,
                NavigationRequest::getId
        );

        Map<Long, NavigationTrip> activeTripByVehicle = latestByVehicle(
                navigationTripRepository.findByVehicleIdInAndStatusIn(vehicleIds, ACTIVE_TRIP_STATUSES),
                NavigationTrip::getVehicleId,
                NavigationTrip::getId
        );

        Map<Long, JobCard> activeJobCardByVehicle = latestByVehicle(
                jobCardRepository.findByVehicle_IdInAndStatusNotIn(vehicleIds, TERMINAL_JOB_CARD_STATUSES),
                jobCard -> jobCard.getVehicle().getId(),
                JobCard::getId
        );

        List<CustomerLiveVehicleJourneySummary> result = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {

            Long vehicleId = vehicle.getId();

            NavigationTrip trip = activeTripByVehicle.get(vehicleId);
            JobCard jobCard = activeJobCardByVehicle.get(vehicleId);
            Booking booking = activeBookingByVehicle.get(vehicleId);
            NavigationRequest pendingRequest = pendingRequestByVehicle.get(vehicleId);

            JourneyDerivation derivation = deriveStage(trip, jobCard, booking, pendingRequest);

            if (derivation == null) {
                // Nothing relevant is happening with this vehicle right
                // now - omitted entirely, never returned as an explicit
                // NO_ACTIVE_JOURNEY row.
                continue;
            }

            result.add(CustomerLiveVehicleJourneySummary.builder()
                    .vehicle(mapper.toVehicle(vehicle))
                    .journeyStage(derivation.stage())
                    .journeyMessage(derivation.message())
                    .booking(toBookingSummary(booking))
                    .navigationTrip(toTripSummary(trip))
                    .jobCard(jobCard == null ? null : mapper.toJobCard(jobCard))
                    .actionRequired(derivation.actionRequired())
                    .primaryAction(derivation.primaryAction())
                    .build());
        }

        return result;
    }

    private Customer getCurrentCustomer() {

        GarageUserPrincipal principal =
                (GarageUserPrincipal) SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return customerRepository
                .findByMobileNumber(principal.getMobile())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found."));
    }

    /**
     * Groups a batch result by vehicleId, keeping only the highest-id
     * (most recently created) row per vehicle - defensive against more
     * than one candidate row existing for the same vehicle at once, which
     * nothing in the schema actually prevents.
     */
    private <T> Map<Long, T> latestByVehicle(
            List<T> rows,
            Function<T, Long> vehicleIdOf,
            Function<T, Long> idOf) {

        Map<Long, T> byVehicle = new HashMap<>();

        for (T row : rows) {
            Long vehicleId = vehicleIdOf.apply(row);
            T existing = byVehicle.get(vehicleId);
            if (existing == null || idOf.apply(row) > idOf.apply(existing)) {
                byVehicle.put(vehicleId, row);
            }
        }

        return byVehicle;
    }

    /**
     * The deterministic stage-priority rule: an active NavigationTrip
     * (the vehicle is physically being moved) always takes precedence
     * over the JobCard's garage-side workflow, because a trip in motion
     * is the more time-critical, more specific thing happening right now
     * - this covers both the pickup leg (no JobCard exists yet) and a
     * delivery leg running concurrently with a JobCard that has already
     * reached READY_FOR_DELIVERY/DELIVERED. JobCard progress is the next
     * priority once no trip is active. A booking with no navigation or
     * job card yet (pickup not requested, or driver not yet assigned) is
     * the lowest-priority "something is happening" signal. Returns null
     * when none of the three applies - the vehicle has no active journey
     * and is omitted from the result entirely.
     */
    private JourneyDerivation deriveStage(
            NavigationTrip trip,
            JobCard jobCard,
            Booking booking,
            NavigationRequest pendingRequest) {

        if (trip != null) {
            return deriveFromTrip(trip);
        }

        if (jobCard != null) {
            return deriveFromJobCard(jobCard);
        }

        if (pendingRequest != null) {
            return new JourneyDerivation(
                    LiveVehicleJourneyStage.BOOKING_CONFIRMED,
                    "Booking confirmed - arranging your pickup driver.",
                    false,
                    PrimaryActionType.NONE
            );
        }

        if (booking != null) {
            return new JourneyDerivation(
                    LiveVehicleJourneyStage.BOOKING_CONFIRMED,
                    "Booking confirmed.",
                    false,
                    PrimaryActionType.NONE
            );
        }

        return null;
    }

    private JourneyDerivation deriveFromTrip(NavigationTrip trip) {

        TripStatus status = trip.getStatus();

        if (status == TripStatus.ASSIGNED || status == TripStatus.ACCEPTED) {

            // NavigationTripServiceImpl's ACCEPTED state (a driver has
            // acknowledged the trip but not yet pressed "start") has no
            // distinct stage in the requested customer-facing set - it is
            // reported as DRIVER_ASSIGNED for its whole duration.
            return new JourneyDerivation(
                    LiveVehicleJourneyStage.DRIVER_ASSIGNED,
                    "A driver has been assigned to your pickup.",
                    false,
                    PrimaryActionType.TRACK_VEHICLE
            );
        }

        // status == IN_PROGRESS - the only remaining value in
        // ACTIVE_TRIP_STATUSES.

        if (trip.getTripType() == TripType.PICKUP) {

            if (trip.getCurrentLeg() == TripLeg.GARAGE_TO_CUSTOMER) {

                if (trip.getArrivedAt() == null) {
                    return new JourneyDerivation(
                            LiveVehicleJourneyStage.DRIVER_EN_ROUTE,
                            "Driver is on the way to pick up your vehicle.",
                            false,
                            PrimaryActionType.TRACK_VEHICLE
                    );
                }

                return new JourneyDerivation(
                        LiveVehicleJourneyStage.DRIVER_ARRIVED,
                        "Driver has arrived at your location.",
                        false,
                        PrimaryActionType.VIEW_PICKUP
                );
            }

            // currentLeg == CUSTOMER_TO_GARAGE. NavigationTripServiceImpl
            // .continueTrip() clears arrivedAt back to null rather than
            // stamping a separate "pickedUpAt" - there is no backend
            // timestamp that distinguishes "just picked up" from "still
            // en route to the garage", so LiveVehicleJourneyStage
            // .VEHICLE_PICKED_UP is never returned; this whole leg is
            // EN_ROUTE_TO_GARAGE, with the pickup acknowledged in the
            // message text instead.
            return new JourneyDerivation(
                    LiveVehicleJourneyStage.EN_ROUTE_TO_GARAGE,
                    "Vehicle picked up - heading to the garage.",
                    false,
                    PrimaryActionType.TRACK_VEHICLE
            );
        }

        // DELIVERY trip in progress - the vehicle is on its way back to
        // the customer. There is no separate requested stage for "driver
        // en route to hand the vehicle back", so this is reported as
        // HANDOVER for the whole return leg, with the message
        // distinguishing "still moving" from "arrived, ready to verify".
        boolean arrived = trip.getArrivedAt() != null;

        return new JourneyDerivation(
                LiveVehicleJourneyStage.HANDOVER,
                arrived
                        ? "Driver has arrived with your vehicle."
                        : "Your vehicle is on its way back to you.",
                false,
                arrived ? PrimaryActionType.COMPLETE_HANDOVER : PrimaryActionType.TRACK_VEHICLE
        );
    }

    private JourneyDerivation deriveFromJobCard(JobCard jobCard) {

        JobCardStatus status = jobCard.getStatus();

        return switch (status) {

            case OPEN -> new JourneyDerivation(
                    LiveVehicleJourneyStage.ARRIVED_AT_GARAGE,
                    "Your vehicle has arrived at the garage.",
                    false,
                    PrimaryActionType.VIEW_PROGRESS
            );

            case INSPECTION_PENDING, INSPECTION_COMPLETED -> new JourneyDerivation(
                    LiveVehicleJourneyStage.INSPECTION,
                    "Inspection is in progress.",
                    false,
                    PrimaryActionType.VIEW_PROGRESS
            );

            case ESTIMATE_PENDING -> new JourneyDerivation(
                    LiveVehicleJourneyStage.ESTIMATE_PENDING,
                    "Your estimate is being prepared.",
                    false,
                    PrimaryActionType.VIEW_PROGRESS
            );

            case WAITING_FOR_APPROVAL -> new JourneyDerivation(
                    LiveVehicleJourneyStage.CUSTOMER_APPROVAL_REQUIRED,
                    "Estimate ready for your approval.",
                    true,
                    PrimaryActionType.REVIEW_ESTIMATE
            );

            // ESTIMATE_APPROVED is a legacy synonym for REPAIR_PENDING -
            // see JobCardStatusValidator's own doc comment; the canonical
            // approveEstimateCanonical() path sets REPAIR_PENDING
            // directly and never writes ESTIMATE_APPROVED, but old rows
            // may still carry it.
            case REPAIR_PENDING, ESTIMATE_APPROVED -> new JourneyDerivation(
                    LiveVehicleJourneyStage.REPAIR_PENDING,
                    "Repair is scheduled.",
                    false,
                    PrimaryActionType.VIEW_REPAIR_PROGRESS
            );

            case REPAIR_IN_PROGRESS -> new JourneyDerivation(
                    LiveVehicleJourneyStage.REPAIR_IN_PROGRESS,
                    "Repair is in progress.",
                    false,
                    PrimaryActionType.VIEW_REPAIR_PROGRESS
            );

            // REPAIR_COMPLETED (and its legacy synonym WORK_COMPLETED)
            // means every RepairTask is done but quality check has not
            // been triggered yet (ServiceWorkflowServiceImpl
            // .performQualityCheck). RepairTask-level detail remains the
            // source of truth for actual task execution - this JobCard
            // status is only used here to pick a customer-facing stage,
            // never to infer individual task completion.
            case REPAIR_COMPLETED, WORK_COMPLETED -> new JourneyDerivation(
                    LiveVehicleJourneyStage.REPAIR_IN_PROGRESS,
                    "Repair completed - quality check starting soon.",
                    false,
                    PrimaryActionType.VIEW_REPAIR_PROGRESS
            );

            case QUALITY_CHECK -> new JourneyDerivation(
                    LiveVehicleJourneyStage.QUALITY_CHECK,
                    "Quality check is in progress.",
                    false,
                    PrimaryActionType.VIEW_PROGRESS
            );

            // READY_FOR_INVOICE/INVOICE_GENERATED and their legacy
            // synonyms INVOICED/PAYMENT_PENDING all precede
            // InvoiceServiceImpl.receivePayment - the customer has
            // something to pay in every one of these states.
            case READY_FOR_INVOICE, INVOICE_GENERATED, INVOICED, PAYMENT_PENDING -> new JourneyDerivation(
                    LiveVehicleJourneyStage.PAYMENT_PENDING,
                    "Payment is required to complete your service.",
                    true,
                    PrimaryActionType.PAY_NOW
            );

            // PAYMENT_COMPLETED is a legacy synonym never written by
            // InvoiceServiceImpl.receivePayment on the canonical path
            // (it moves straight to READY_FOR_DELIVERY), kept here only
            // for old rows.
            case READY_FOR_DELIVERY, PAYMENT_COMPLETED -> new JourneyDerivation(
                    LiveVehicleJourneyStage.VEHICLE_READY,
                    "Your vehicle is ready.",
                    false,
                    PrimaryActionType.VIEW_VEHICLE
            );

            case DELIVERED -> new JourneyDerivation(
                    LiveVehicleJourneyStage.COMPLETED,
                    "Service completed.",
                    false,
                    PrimaryActionType.NONE
            );

            // CLOSED/CANCELLED are excluded by the active-fetch query and
            // can never reach this method. This default only guards
            // against a future JobCardStatus value this method doesn't
            // know about yet - fails safe with a generic message rather
            // than throwing.
            default -> new JourneyDerivation(
                    LiveVehicleJourneyStage.ARRIVED_AT_GARAGE,
                    "Your vehicle is being serviced.",
                    false,
                    PrimaryActionType.VIEW_PROGRESS
            );
        };
    }

    private VehicleJourneyBookingSummary toBookingSummary(Booking booking) {

        if (booking == null) {
            return null;
        }

        return VehicleJourneyBookingSummary.builder()
                .id(booking.getId())
                .status(booking.getStatus())
                .pickupRequested(booking.isPickupRequested())
                .build();
    }

    private VehicleJourneyTripSummary toTripSummary(NavigationTrip trip) {

        if (trip == null) {
            return null;
        }

        boolean handoverPending = vehicleHandoverRepository
                .findFirstByTripIdAndStatusOrderByCreatedAtDesc(trip.getId(), HandoverStatus.PENDING)
                .isPresent();

        return VehicleJourneyTripSummary.builder()
                .id(trip.getId())
                .status(trip.getStatus())
                .tripType(trip.getTripType())
                .currentLeg(trip.getCurrentLeg())
                .arrivedAt(trip.getArrivedAt())
                .handoverPending(handoverPending)
                .build();
    }

    private record JourneyDerivation(
            LiveVehicleJourneyStage stage,
            String message,
            boolean actionRequired,
            PrimaryActionType primaryAction
    ) {
    }
}
