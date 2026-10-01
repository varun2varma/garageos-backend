package com.garageos.modules.notification.service;

import com.garageos.core.enums.identity.RoleCode;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.notification.NotificationEventType;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.garage.repository.GarageRepository;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.notification.event.NotificationFacts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.garageos.modules.notification.service.NotificationRecipientResolver.Audience.*;

/**
 * Single place that decides who is told about an event.
 *
 * Tenant rule: every STAFF recipient (owner, manager, service advisor,
 * technician, driver) must belong to the event's garage, using the same
 * tenant source the security principal uses (users.garage_id) - the owner
 * is resolved through garage.owner_user_id. Customers are cross-garage
 * records and are resolved only from the event's own customer id.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRecipientResolver {

    enum Audience { CUSTOMER, TECHNICIANS, DRIVER, OWNER, MANAGERS }

    private static final Map<NotificationEventType, Set<Audience>> AUDIENCES =
            new EnumMap<>(NotificationEventType.class);

    static {
        AUDIENCES.put(NotificationEventType.BOOKING_REQUESTED, EnumSet.of(OWNER, MANAGERS));
        AUDIENCES.put(NotificationEventType.BOOKING_CREATED_BY_STAFF, EnumSet.of(CUSTOMER));
        AUDIENCES.put(NotificationEventType.BOOKING_CONFIRMED, EnumSet.of(CUSTOMER));
        AUDIENCES.put(NotificationEventType.BOOKING_REJECTED, EnumSet.of(CUSTOMER));
        AUDIENCES.put(NotificationEventType.BOOKING_CANCELLED, EnumSet.of(CUSTOMER, MANAGERS));

        AUDIENCES.put(NotificationEventType.PICKUP_DRIVER_ASSIGNED, EnumSet.of(DRIVER, CUSTOMER));
        AUDIENCES.put(NotificationEventType.TRIP_ACCEPTED, EnumSet.of(CUSTOMER, MANAGERS));
        AUDIENCES.put(NotificationEventType.TRIP_STARTED, EnumSet.of(CUSTOMER, MANAGERS));
        AUDIENCES.put(NotificationEventType.DRIVER_ARRIVED, EnumSet.of(CUSTOMER));
        AUDIENCES.put(NotificationEventType.HANDOVER_CODE_GENERATED, EnumSet.of(DRIVER));
        AUDIENCES.put(NotificationEventType.HANDOVER_VERIFIED, EnumSet.of(CUSTOMER, MANAGERS));
        AUDIENCES.put(NotificationEventType.TRIP_COMPLETED, EnumSet.of(CUSTOMER, MANAGERS));

        AUDIENCES.put(NotificationEventType.JOB_CARD_CREATED, EnumSet.of(CUSTOMER, OWNER));

        AUDIENCES.put(NotificationEventType.INSPECTION_STARTED, EnumSet.of(CUSTOMER));
        AUDIENCES.put(NotificationEventType.INSPECTION_COMPLETED, EnumSet.of(CUSTOMER, MANAGERS));

        AUDIENCES.put(NotificationEventType.ESTIMATE_READY_FOR_APPROVAL, EnumSet.of(CUSTOMER));
        AUDIENCES.put(NotificationEventType.ESTIMATE_APPROVED, EnumSet.of(CUSTOMER, MANAGERS, OWNER));
        AUDIENCES.put(NotificationEventType.ESTIMATE_REJECTED, EnumSet.of(MANAGERS, OWNER));

        AUDIENCES.put(NotificationEventType.REPAIR_TASK_ASSIGNED, EnumSet.of(TECHNICIANS));
        AUDIENCES.put(NotificationEventType.REPAIR_TASK_STARTED, EnumSet.of(MANAGERS));
        AUDIENCES.put(NotificationEventType.REPAIR_TASK_COMPLETED, EnumSet.of(MANAGERS));

        AUDIENCES.put(NotificationEventType.JOB_REPAIR_STARTED, EnumSet.of(CUSTOMER));
        AUDIENCES.put(NotificationEventType.JOB_REPAIR_COMPLETED, EnumSet.of(CUSTOMER, MANAGERS, OWNER));

        AUDIENCES.put(NotificationEventType.QUALITY_CHECK_STARTED, EnumSet.of(MANAGERS));
        AUDIENCES.put(NotificationEventType.QUALITY_CHECK_PASSED, EnumSet.of(MANAGERS));
        AUDIENCES.put(NotificationEventType.QUALITY_CHECK_FAILED, EnumSet.of(TECHNICIANS, MANAGERS, OWNER));

        AUDIENCES.put(NotificationEventType.INVOICE_GENERATED, EnumSet.of(CUSTOMER, MANAGERS, OWNER));
        AUDIENCES.put(NotificationEventType.INVOICE_ACCEPTED, EnumSet.of(MANAGERS, OWNER));
        AUDIENCES.put(NotificationEventType.PAYMENT_RECEIVED, EnumSet.of(CUSTOMER, MANAGERS, OWNER));

        AUDIENCES.put(NotificationEventType.VEHICLE_READY_FOR_DELIVERY, EnumSet.of(CUSTOMER, MANAGERS, OWNER));
        AUDIENCES.put(NotificationEventType.VEHICLE_DELIVERED, EnumSet.of(CUSTOMER, MANAGERS, OWNER));
        AUDIENCES.put(NotificationEventType.JOB_CLOSED, EnumSet.of(OWNER));
    }

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final GarageRepository garageRepository;

    @Transactional(readOnly = true)
    public List<NotificationRecipient> resolve(
            NotificationEventType type,
            Long garageId,
            Map<String, Object> facts) {

        Set<Audience> audiences = AUDIENCES.getOrDefault(type, Set.of());

        // userId -> persona; first audience that claims a user wins.
        Map<Long, NotificationPersona> recipients = new LinkedHashMap<>();

        if (audiences.contains(CUSTOMER)) {
            resolveCustomer(asLong(facts.get(NotificationFacts.CUSTOMER_ID)))
                    .ifPresent(id -> recipients.putIfAbsent(id, NotificationPersona.CUSTOMER));
        }

        if (garageId != null) {

            if (audiences.contains(TECHNICIANS)) {
                for (Long id : asLongList(facts.get(NotificationFacts.TECHNICIAN_USER_IDS))) {
                    staffOfGarage(id, garageId)
                            .ifPresent(u -> recipients.putIfAbsent(u.getId(), NotificationPersona.TECHNICIAN));
                }
            }

            if (audiences.contains(DRIVER)) {
                staffOfGarage(asLong(facts.get(NotificationFacts.DRIVER_USER_ID)), garageId)
                        .ifPresent(u -> recipients.putIfAbsent(u.getId(), NotificationPersona.DRIVER));
            }

            if (audiences.contains(OWNER)) {
                garageRepository.findById(garageId)
                        .map(g -> g.getOwnerUserId())
                        .flatMap(this::activeUser)
                        .ifPresent(u -> recipients.putIfAbsent(u.getId(), NotificationPersona.OWNER));
            }

            if (audiences.contains(MANAGERS)) {
                for (RoleCode role : List.of(RoleCode.MANAGER, RoleCode.SERVICE_ADVISOR)) {
                    for (User u : userRepository.findByGarageIdAndRoleAndStatus(
                            garageId, role, UserStatus.ACTIVE)) {
                        if (Objects.equals(u.getGarageId(), garageId)) {
                            recipients.putIfAbsent(u.getId(), NotificationPersona.STAFF);
                        }
                    }
                }
            }
        }

        // Actor self-notification suppression.
        Long actorId = asLong(facts.get(NotificationFacts.ACTOR_USER_ID));
        if (actorId != null) {
            recipients.remove(actorId);
        }

        List<NotificationRecipient> result = new ArrayList<>();
        recipients.forEach((id, persona) -> result.add(new NotificationRecipient(id, persona)));
        return result;
    }

    private java.util.Optional<Long> resolveCustomer(Long customerId) {

        if (customerId == null) {
            return java.util.Optional.empty();
        }

        return customerRepository.findById(customerId)
                .map(c -> c.getMobileNumber())
                .filter(m -> !m.isBlank())
                .flatMap(userRepository::findByMobile)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> u.getUserRoles().stream()
                        .anyMatch(ur -> ur.getRole() != null
                                && ur.getRole().getCode() == RoleCode.CUSTOMER))
                .map(User::getId);
    }

    private java.util.Optional<User> staffOfGarage(Long userId, Long garageId) {
        return activeUser(userId)
                .filter(u -> Objects.equals(u.getGarageId(), garageId));
    }

    private java.util.Optional<User> activeUser(Long userId) {
        if (userId == null) {
            return java.util.Optional.empty();
        }
        return userRepository.findById(userId)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE);
    }

    private static Long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }

    private static List<Long> asLongList(Object value) {
        List<Long> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) {
                Long l = asLong(o);
                if (l != null) {
                    out.add(l);
                }
            }
        }
        return out;
    }
}
