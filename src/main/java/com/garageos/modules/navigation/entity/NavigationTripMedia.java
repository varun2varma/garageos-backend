package com.garageos.modules.navigation.entity;

import com.garageos.core.enums.navigation.TripMediaStage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "navigation_trip_media",
        indexes = {

                @Index(
                        name = "idx_trip_media_trip",
                        columnList = "trip_id"
                ),

                @Index(
                        name = "idx_trip_media_stage",
                        columnList = "media_stage"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavigationTripMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "trip_id",
            nullable = false
    )
    private Long tripId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "media_stage",
            nullable = false
    )
    private TripMediaStage mediaStage;

    @Column(
            name = "storage_key",
            nullable = false
    )
    private String storageKey;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(
            name = "captured_by",
            nullable = false
    )
    private Long capturedBy;

    @Column(
            name = "captured_at",
            nullable = false
    )
    private LocalDateTime capturedAt;

    private Double latitude;

    private Double longitude;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (capturedAt == null) {

            capturedAt =
                    LocalDateTime.now();
        }

        if (createdAt == null) {

            createdAt =
                    LocalDateTime.now();
        }
    }
}