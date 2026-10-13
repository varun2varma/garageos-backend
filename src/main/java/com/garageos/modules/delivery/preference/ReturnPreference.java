package com.garageos.modules.delivery.preference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The customer's persisted choice for returning the vehicle of one job card (V75). One row per job card. A
 * DRIVER_DELIVERY choice is mirrored into the existing NavigationRequest (type DELIVERY) so the garage assigns a
 * driver and the customer tracks it through the existing trip/tracking machinery; this row is the authoritative
 * record of the choice itself (including the collect-at-garage choice, which has no trip).
 */
@Entity
@Table(name = "job_card_return_preference")
@Getter
@Setter
@NoArgsConstructor
public class ReturnPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_card_id", nullable = false, unique = true)
    private Long jobCardId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnMethod method;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_latitude", precision = 10, scale = 7)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 10, scale = 7)
    private BigDecimal deliveryLongitude;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
