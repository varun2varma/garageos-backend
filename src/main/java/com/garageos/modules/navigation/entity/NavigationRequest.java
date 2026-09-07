package com.garageos.modules.navigation.entity;

import com.garageos.core.enums.navigation.NavigationRequestStatus;
import com.garageos.core.enums.navigation.NavigationRequestType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "navigation_request",
        indexes = {

                @Index(
                        name = "idx_navigation_request_customer",
                        columnList = "customer_id"
                ),

                @Index(
                        name = "idx_navigation_request_vehicle",
                        columnList = "vehicle_id"
                ),

                @Index(
                        name = "idx_navigation_request_garage",
                        columnList = "garage_id"
                ),

                @Index(
                        name = "idx_navigation_request_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavigationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "customer_id",
            nullable = false
    )
    private Long customerId;

    @Column(
            name = "vehicle_id",
            nullable = false
    )
    private Long vehicleId;

    @Column(
            name = "garage_id",
            nullable = false
    )
    private Long garageId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "request_type",
            nullable = false
    )
    private NavigationRequestType requestType;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(
            name = "scheduled_at",
            nullable = false
    )
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false
    )
    private NavigationRequestStatus status;

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
            status =
                    NavigationRequestStatus.REQUESTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}