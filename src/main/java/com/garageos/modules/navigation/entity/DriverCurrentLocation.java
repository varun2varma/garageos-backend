package com.garageos.modules.navigation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "driver_current_location",
        indexes = {
                @Index(
                        name = "idx_current_location_driver",
                        columnList = "driver_id"
                ),
                @Index(
                        name = "idx_current_location_trip",
                        columnList = "trip_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverCurrentLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double speed;

    private Double heading;

    private Double accuracy;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}