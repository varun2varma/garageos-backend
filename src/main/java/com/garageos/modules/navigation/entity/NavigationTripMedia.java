package com.garageos.modules.navigation.entity;

import com.garageos.core.enums.navigation.TripMediaShotType;
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

    /** Null for rows captured before this field existed (V47). */
    @Enumerated(EnumType.STRING)
    @Column(name = "shot_type")
    private TripMediaShotType shotType;

    @Column(
            name = "storage_key",
            nullable = false
    )
    private String storageKey;

    @Column(name = "file_name")
    private String fileName;

    /** Client-generated id of the captured file; makes retries idempotent (V71). */
    @Column(name = "client_upload_id", length = 80)
    private String clientUploadId;

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

    /** Historical identity snapshot of the authenticated uploader (V74). */
    @Column(name = "captured_by_name", length = 200)
    private String capturedByName;

    /** DEVICE when the validated device capture time was kept, SERVER when it fell back to receipt time (V74). */
    @Column(name = "captured_at_source", length = 10)
    private String capturedAtSource;

    /** GPS accuracy in metres of the fix behind latitude/longitude; null when unavailable (V74). */
    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    /** Resolved address/place name when the device supplied one; null when unavailable (V74). */
    @Column(name = "location_address", length = 500)
    private String locationAddress;

    /** Explicit customer-visibility decision (V79). Final reports include a trip photo only when true. */
    @Column(name = "customer_visible", nullable = false)
    private boolean customerVisible;

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