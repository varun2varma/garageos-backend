package com.garageos.modules.booking.entity;

import com.garageos.core.enums.booking.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Customer service-request intent, distinct from {@code JobCard} (which
 * only exists once the garage has actually opened work on the vehicle).
 * Stores plain ids for garage/customer/vehicle rather than JPA
 * relationships — matches the existing navigation module's style
 * (NavigationRequest/NavigationTrip), which this module sits alongside.
 */
@Entity
@Table(
        name = "booking",
        indexes = {

                @Index(
                        name = "idx_booking_customer",
                        columnList = "customer_id"
                ),

                @Index(
                        name = "idx_booking_garage",
                        columnList = "garage_id"
                ),

                @Index(
                        name = "idx_booking_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "garage_id", nullable = false)
    private Long garageId;

    @Column(name = "service_description", nullable = false, length = 1000)
    private String serviceDescription;

    @Column(name = "concerns", length = 2000)
    private String concerns;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "pickup_requested", nullable = false)
    private boolean pickupRequested;

    /**
     * Human-readable pickup location - descriptive metadata for the
     * customer, driver and garage to read. Deliberately NOT the source of
     * truth for navigation: see the coordinate pair below.
     */
    @Column(name = "pickup_address", length = 500)
    private String pickupAddress;

    /**
     * Canonical pickup coordinates. These, not {@link #pickupAddress},
     * are what a driver navigates to, and what is carried onto the
     * NavigationRequest when the garage confirms the booking.
     *
     * Null when pickup was not requested, and null for bookings created
     * before V41.
     */
    @Column(name = "pickup_latitude", precision = 10, scale = 7)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", precision = 10, scale = 7)
    private BigDecimal pickupLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "garage_remarks", length = 1000)
    private String garageRemarks;

    /**
     * Set when {@link #pickupRequested} is true and the garage confirms the
     * booking — the created {@code NavigationRequest}'s id, so the pickup
     * logistics that follow reuse the existing Navigation module instead of
     * a second one. Null until then, and always null when pickup wasn't
     * requested.
     */
    @Column(name = "navigation_request_id")
    private Long navigationRequestId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = BookingStatus.REQUESTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
