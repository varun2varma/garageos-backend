package com.garageos.modules.chat.service;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.modules.booking.entity.Booking;
import com.garageos.modules.booking.repository.BookingRepository;
import com.garageos.modules.chat.entity.ChatConversation.ScopeType;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.repository.JobAssignmentRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.navigation.entity.NavigationRequest;
import com.garageos.modules.navigation.entity.NavigationTrip;
import com.garageos.modules.navigation.repository.NavigationRequestRepository;
import com.garageos.modules.navigation.repository.NavigationTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The single place that decides who may talk in a booking or job-card conversation. Participants are derived from the
 * live relationships at every call, never stored:
 * <ul>
 *   <li><b>Customer</b>: the booking's / job card's customer (chat needs a user account; a walk-in customer without
 *       one is listed for calling only).</li>
 *   <li><b>Garage</b>: the garage owner and the ACTIVE managers and service advisors of the booking's / job card's
 *       garage. Other garages' staff are never participants.</li>
 *   <li><b>Driver</b>: the driver of a trip linked to the booking's pickup request / the job card's pickup or delivery
 *       requests, only while that trip is ASSIGNED, ACCEPTED or IN_PROGRESS. A completed or cancelled (reassigned)
 *       trip ends the driver's access.</li>
 *   <li><b>Technician</b> (job-card scope only): a technician with a job assignment on the job card that is not
 *       COMPLETED or CANCELLED.</li>
 * </ul>
 * SUPER_ADMIN and unrelated users are never participants.
 */
@Component
@RequiredArgsConstructor
public class ChatParticipantResolver {

    public record Participant(Long userId, String role, String name, String mobile) {
    }

    public record Scope(ScopeType type, Long id, Long garageId, Long jobCardId, List<Participant> participants) {

        public boolean isParticipant(Long userId) {
            return userId != null && participants.stream().anyMatch(p -> userId.equals(p.userId()));
        }

        public Optional<Participant> participant(Long userId) {
            return participants.stream().filter(p -> userId != null && userId.equals(p.userId())).findFirst();
        }
    }

    private static final Set<TripStatus> LIVE_TRIP = EnumSet.of(TripStatus.ASSIGNED, TripStatus.ACCEPTED, TripStatus.IN_PROGRESS);
    private static final Set<JobAssignmentStatus> ENDED_ASSIGNMENT =
            EnumSet.of(JobAssignmentStatus.COMPLETED, JobAssignmentStatus.CANCELLED);

    private final BookingRepository bookingRepository;
    private final JobCardRepository jobCardRepository;
    private final CustomerRepository customerRepository;
    private final GarageRepository garageRepository;
    private final UserRepository userRepository;
    private final NavigationRequestRepository requestRepository;
    private final NavigationTripRepository tripRepository;
    private final JobAssignmentRepository assignmentRepository;

    /** The conversation scope a trip belongs to: its job card once one exists, otherwise its booking. */
    public record ScopeRef(ScopeType type, Long id) {
    }

    /** The job card with this number, for clients (the customer app) that know the number but not the id. */
    public Optional<Long> jobCardIdByNumber(String jobCardNumber) {
        return jobCardNumber == null || jobCardNumber.isBlank()
                ? Optional.empty()
                : jobCardRepository.findByJobCardNumber(jobCardNumber.trim()).map(JobCard::getId);
    }

    public Optional<ScopeRef> scopeOfTrip(Long tripId) {

        if (tripId == null) {
            return Optional.empty();
        }

        NavigationTrip trip = tripRepository.findById(tripId).orElse(null);

        if (trip == null || trip.getNavigationRequestId() == null) {
            return Optional.empty();
        }

        NavigationRequest request = requestRepository.findById(trip.getNavigationRequestId()).orElse(null);

        if (request == null) {
            return Optional.empty();
        }

        if (request.getJobCardId() != null) {
            return Optional.of(new ScopeRef(ScopeType.JOB_CARD, request.getJobCardId()));
        }

        return bookingRepository.findByNavigationRequestId(request.getId())
                .map(b -> new ScopeRef(ScopeType.BOOKING, b.getId()));
    }

    public Optional<Scope> resolve(ScopeType type, Long scopeId) {

        if (type == null || scopeId == null) {
            return Optional.empty();
        }

        return type == ScopeType.BOOKING ? forBooking(scopeId) : forJobCard(scopeId);
    }

    private Optional<Scope> forBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if (booking == null) {
            return Optional.empty();
        }

        Map<Long, Participant> out = new LinkedHashMap<>();
        addCustomer(out, booking.getCustomerId());
        addGarageStaff(out, booking.getGarageId());

        if (booking.getNavigationRequestId() != null) {
            addLiveDrivers(out, List.of(booking.getNavigationRequestId()));
        }

        return Optional.of(new Scope(ScopeType.BOOKING, bookingId, booking.getGarageId(), null, new ArrayList<>(out.values())));
    }

    private Optional<Scope> forJobCard(Long jobCardId) {

        JobCard jobCard = jobCardRepository.findById(jobCardId).orElse(null);

        if (jobCard == null || jobCard.getGarage() == null) {
            return Optional.empty();
        }

        Long garageId = jobCard.getGarage().getId();

        Map<Long, Participant> out = new LinkedHashMap<>();
        addCustomer(out, jobCard.getCustomer() == null ? null : jobCard.getCustomer().getId());
        addGarageStaff(out, garageId);

        List<Long> requestIds = requestRepository.findByJobCardId(jobCardId).stream().map(NavigationRequest::getId).toList();
        if (!requestIds.isEmpty()) {
            addLiveDrivers(out, requestIds);
        }

        for (JobAssignment a : assignmentRepository.findByJobCardId(jobCardId)) {
            if (a.getUser() != null && a.getStatus() != null && !ENDED_ASSIGNMENT.contains(a.getStatus())
                    && garageId.equals(a.getUser().getGarageId())) {
                User u = a.getUser();
                out.putIfAbsent(u.getId(), participantOf(u, "TECHNICIAN"));
            }
        }

        return Optional.of(new Scope(ScopeType.JOB_CARD, jobCardId, garageId, jobCardId, new ArrayList<>(out.values())));
    }

    private void addCustomer(Map<Long, Participant> out, Long customerId) {

        if (customerId == null) {
            return;
        }

        Customer customer = customerRepository.findById(customerId).orElse(null);

        if (customer == null) {
            return;
        }

        String name = ((customer.getFirstName() == null ? "" : customer.getFirstName())
                + (customer.getLastName() == null ? "" : " " + customer.getLastName())).trim();

        User account = customer.getMobileNumber() == null || customer.getMobileNumber().isBlank()
                ? null
                : userRepository.findByMobile(customer.getMobileNumber())
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> u.getUserRoles().stream()
                        .anyMatch(ur -> ur.getRole() != null && ur.getRole().getCode() == RoleCode.CUSTOMER))
                .orElse(null);

        if (account != null) {
            out.putIfAbsent(account.getId(), new Participant(account.getId(), "CUSTOMER", name, customer.getMobileNumber()));
        } else {
            // No account: callable, but cannot chat. A negative key keeps it distinct from real user ids.
            out.putIfAbsent(-customerId, new Participant(null, "CUSTOMER", name, customer.getMobileNumber()));
        }
    }

    private void addGarageStaff(Map<Long, Participant> out, Long garageId) {

        if (garageId == null) {
            return;
        }

        garageRepository.findById(garageId).map(g -> g.getOwnerUserId()).flatMap(this::activeUser)
                .ifPresent(u -> out.putIfAbsent(u.getId(), participantOf(u, "OWNER")));

        for (RoleCode role : List.of(RoleCode.MANAGER, RoleCode.SERVICE_ADVISOR)) {
            for (User u : userRepository.findByGarageIdAndRoleAndStatus(garageId, role, UserStatus.ACTIVE)) {
                if (garageId.equals(u.getGarageId())) {
                    out.putIfAbsent(u.getId(), participantOf(u, role.name()));
                }
            }
        }
    }

    private void addLiveDrivers(Map<Long, Participant> out, List<Long> requestIds) {

        for (NavigationTrip trip : tripRepository.findByNavigationRequestIdIn(requestIds)) {
            if (trip.getDriverId() != null && trip.getStatus() != null && LIVE_TRIP.contains(trip.getStatus())) {
                activeUser(trip.getDriverId()).ifPresent(u -> out.putIfAbsent(u.getId(), participantOf(u, "DRIVER")));
            }
        }
    }

    private Optional<User> activeUser(Long userId) {
        return userId == null ? Optional.empty()
                : userRepository.findById(userId).filter(u -> u.getStatus() == UserStatus.ACTIVE);
    }

    private static Participant participantOf(User u, String role) {
        String name = ((u.getFirstName() == null ? "" : u.getFirstName())
                + (u.getLastName() == null ? "" : " " + u.getLastName())).trim();
        return new Participant(u.getId(), role, name.isEmpty() ? role : name, u.getMobile());
    }
}
