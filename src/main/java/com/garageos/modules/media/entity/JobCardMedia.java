package com.garageos.modules.media.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_card_media",
        indexes = {
                @Index(
                        name = "idx_job_card_media_job_card",
                        columnList = "job_card_id"
                ),
                @Index(
                        name = "idx_job_card_media_stage",
                        columnList = "job_card_id, media_stage"
                ),
                @Index(
                        name = "idx_job_card_media_type",
                        columnList = "job_card_id, media_type"
                ),
                @Index(
                        name = "idx_job_card_media_drive_file",
                        columnList = "drive_file_id"
                ),
                @Index(
                        name = "idx_job_card_media_repair_task",
                        columnList = "repair_task_id"
                ),
                @Index(
                        name = "idx_job_card_media_visibility",
                        columnList = "job_card_id, visibility"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_card_id", nullable = false)
    private Long jobCardId;

    @Column(name = "repair_task_id")
    private Long repairTaskId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "drive_file_id", nullable = false, length = 255)
    private String driveFileId;

    @Column(name = "drive_web_view_link")
    private String driveWebViewLink;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType;

    @Column(name = "media_stage", nullable = false, length = 30)
    private String mediaStage;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private String visibility = "INTERNAL";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}