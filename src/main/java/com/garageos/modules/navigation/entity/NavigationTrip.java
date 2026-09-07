package com.garageos.modules.navigation.entity;

import com.garageos.core.enums.navigation.TripLeg;
import com.garageos.core.enums.navigation.TripStatus;
import com.garageos.core.enums.navigation.TripType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "navigation_trip",
        indexes = {

                @Index(
                        name = "idx_navigation_trip_request",
                        columnList = "navigation_request_id"
                ),

                @Index(
                        name = "idx_navigation_trip_driver",
                        columnList = "driver_id"
                ),

                @Index(
                        name = "idx_navigation_trip_vehicle",
                        columnList = "vehicle_id"
                ),

                @Index(
                        name = "idx_navigation_trip_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavigationTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "navigation_request_id",
            nullable = false
    )
    private Long navigationRequestId;

    @Column(
            name = "vehicle_id",
            nullable = false
    )
    private Long vehicleId;

    @Column(name = "driver_id")
    private Long driverId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "trip_type",
            nullable = false
    )
    private TripType tripType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "current_leg",
            nullable = false
    )
    private TripLeg currentLeg;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false
    )
    private TripStatus status;

    @Column(name = "source_address")
    private String sourceAddress;

    @Column(name = "destination_address")
    private String destinationAddress;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = TripStatus.ASSIGNED;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}