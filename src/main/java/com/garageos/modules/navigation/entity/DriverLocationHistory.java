package com.garageos.modules.navigation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "driver_location_history",
        indexes = {
                @Index(
                        name = "idx_location_history_driver",
                        columnList = "driver_id"
                ),
                @Index(
                        name = "idx_location_history_trip",
                        columnList = "trip_id"
                ),
                @Index(
                        name = "idx_location_history_time",
                        columnList = "location_time"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocationHistory {

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

    @Column(name = "location_time", nullable = false)
    private LocalDateTime locationTime;
}