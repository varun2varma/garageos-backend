package com.garageos.modules.handover.entity;

import com.garageos.core.enums.navigation.HandoverStatus;
import com.garageos.core.enums.navigation.TripType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A server-generated confirmation-code handover event for one
 * NavigationTrip leg (PICKUP or DELIVERY). At most one PENDING row exists
 * per trip at a time - {@link com.garageos.modules.handover.service.impl.HandoverServiceImpl}
 * expires the previous row before issuing a new code, and a verified row
 * is never treated as "active" again, which is what makes code reuse after
 * a successful verification impossible without a fresh customer-issued
 * code.
 *
 * The plaintext code itself is never persisted - only {@link #codeHash}
 * (hashed with the application's existing PasswordEncoder bean, the same
 * one used for user login credentials).
 */
@Entity
@Table(
        name = "vehicle_handover",
        indexes = {

                @Index(
                        name = "idx_vehicle_handover_trip",
                        columnList = "trip_id"
                ),

                @Index(
                        name = "idx_vehicle_handover_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripType direction;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HandoverStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by_driver_id")
    private Long verifiedByDriverId;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

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
            status = HandoverStatus.PENDING;
        }

        if (failedAttempts < 0) {
            failedAttempts = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
