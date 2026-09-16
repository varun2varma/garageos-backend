package com.garageos.modules.media.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.media.MediaStage;
import com.garageos.core.exception.MediaException;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobassignment.entity.JobAssignment;
import com.garageos.modules.jobassignment.repository.JobAssignmentRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.repository.JobCardMediaRepository;
import com.garageos.modules.media.service.GoogleDriveFileService;
import com.garageos.modules.media.service.GoogleDriveFolderService;
import com.garageos.modules.repairtask.entity.RepairTask;
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import com.google.api.services.drive.model.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Media upload across every stage.
 *
 * The defect this pins: DURING_REPAIR media REQUIRED a repairTaskId. No
 * client ever sent one - the Job Card screen uploads against a job card
 * and a stage, which is all it knows - so every repair photo and video
 * was rejected before it reached Google Drive. The rejection was an
 * IllegalArgumentException, which had no handler and surfaced as a bare
 * 500 "Something went wrong.", and the Photos & Videos section stayed
 * permanently empty for repair media.
 *
 * Also pinned: media is addressed by stable ids and a stage only. No
 * task name, job card number or file name takes part in the decision, so
 * an arbitrary, duplicated, renamed or empty repair task name cannot
 * break an upload.
 */
@ExtendWith(MockitoExtension.class)
class MediaUploadStageTest {

    @Mock private JobCardRepository jobCardRepository;
    @Mock private JobCardMediaRepository jobCardMediaRepository;
    @Mock private RepairTaskRepository repairTaskRepository;
    @Mock private JobAssignmentRepository jobAssignmentRepository;
    @Mock private GoogleDriveFolderService folderService;
    @Mock private GoogleDriveFileService googleDriveFileService;

    @InjectMocks
    private MediaServiceImpl service;

    private static final Long JOB_CARD_ID = 100L;
    private static final Long GARAGE_ID = 1L;
    private static final Long TECHNICIAN_ID = 10L;

    private JobCard jobCard;

    @BeforeEach
    void setUp() throws Exception {
        Garage garage = new Garage();
        garage.setId(GARAGE_ID);
        garage.setGarageCode("G001");

        jobCard = new JobCard();
        jobCard.setId(JOB_CARD_ID);
        jobCard.setGarage(garage);
        jobCard.setJobCardNumber("G001-JC-001");

        lenient().when(jobCardRepository.findById(JOB_CARD_ID))
                .thenReturn(Optional.of(jobCard));

        lenient().when(jobCardMediaRepository
                        .findByJobCardIdAndMediaStageOrderByCreatedAtAsc(any(), anyString()))
                .thenReturn(List.of());

        File folder = new File();
        folder.setId("folder-123");
        folder.setName("DURING_REPAIR");
        lenient().when(folderService.getOrCreateStageFolder(anyString(), anyString(), anyString()))
                .thenReturn(folder);

        File uploaded = new File();
        uploaded.setId("drive-file-123");
        uploaded.setWebViewLink("https://drive.example/file/123");
        lenient().when(googleDriveFileService.uploadFile(any(), anyString(), anyString()))
                .thenReturn(uploaded);

        lenient().when(jobCardMediaRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        authenticateAsManager();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsManager() {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                2L, GARAGE_ID, "mgr", "hash", "First", "Last", "m@test.io", "9999999999",
                false, UserStatus.ACTIVE, Set.of("MANAGER"), Set.of(), List.of());
        stubPrincipal(principal);
    }

    private void authenticateAsTechnician() {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                TECHNICIAN_ID, GARAGE_ID, "tech", "hash", "First", "Last",
                "t@test.io", "8888888888", false, UserStatus.ACTIVE,
                Set.of("TECHNICIAN"), Set.of(), List.of());
        stubPrincipal(principal);

        User technician = new User();
        technician.setId(TECHNICIAN_ID);
        JobAssignment assignment = new JobAssignment();
        assignment.setUser(technician);
        assignment.setStatus(JobAssignmentStatus.IN_PROGRESS);

        lenient().when(jobAssignmentRepository.findByJobCardId(JOB_CARD_ID))
                .thenReturn(List.of(assignment));
    }

    private void stubPrincipal(GarageUserPrincipal principal) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private MultipartFile photo() {
        return new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "binary".getBytes());
    }

    private MultipartFile video() {
        return new MockMultipartFile(
                "file", "clip.mp4", "video/mp4", "binary".getBytes());
    }

    /**
     * RepairTask carries no task-name column of its own - the nearest
     * display strings on it are technicianName and the linked
     * EstimateItem's description. Either way they are display strings,
     * and none of them may take part in an upload decision.
     */
    private RepairTask repairTaskWithName(Long id, String displayName) {
        RepairTask task = new RepairTask();
        task.setId(id);
        task.setJobCard(jobCard);
        task.setTechnicianName(displayName);
        return task;
    }

    // ---- The defect ----

    @Test
    void repairPhoto_withoutARepairTask_isAccepted() {

        // This is the exact call the Job Card screen makes. It used to be
        // rejected outright.
        JobCardMedia saved = service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, null, photo());

        assertThat(saved.getJobCardId()).isEqualTo(JOB_CARD_ID);
        assertThat(saved.getMediaStage()).isEqualTo(MediaStage.DURING_REPAIR.name());
        assertThat(saved.getRepairTaskId()).isNull();
        assertThat(saved.getDriveFileId()).isEqualTo("drive-file-123");
    }

    @Test
    void repairVideo_withoutARepairTask_isAccepted() {

        JobCardMedia saved = service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, null, video());

        assertThat(saved.getMediaType()).isEqualTo("VIDEO");
        assertThat(saved.getMediaStage()).isEqualTo(MediaStage.DURING_REPAIR.name());
    }

    @Test
    void repairMedia_isInternalByDefault() {

        // Unchanged behaviour, re-pinned: repair media is internal until
        // someone deliberately makes it customer-visible.
        JobCardMedia saved = service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, null, photo());

        assertThat(saved.getVisibility()).isEqualTo("INTERNAL");
    }

    // ---- Every stage works through the same path ----

    @Test
    void inspectionMedia_isAccepted() {

        JobCardMedia saved = service.uploadMedia(
                JOB_CARD_ID, MediaStage.BEFORE_SERVICE, null, photo());

        assertThat(saved.getMediaStage()).isEqualTo(MediaStage.BEFORE_SERVICE.name());
        assertThat(saved.getVisibility()).isEqualTo("CUSTOMER_VISIBLE");
    }

    @Test
    void afterRepairMedia_isAccepted() {

        JobCardMedia saved = service.uploadMedia(
                JOB_CARD_ID, MediaStage.AFTER_REPAIR, null, photo());

        assertThat(saved.getMediaStage()).isEqualTo(MediaStage.AFTER_REPAIR.name());
        assertThat(saved.getVisibility()).isEqualTo("CUSTOMER_VISIBLE");
    }

    // ---- Per-task media still works when a task IS supplied ----

    @Test
    void repairMedia_withARepairTask_isStillTiedToThatTask() {

        when(repairTaskRepository.findById(55L))
                .thenReturn(Optional.of(repairTaskWithName(55L, "Brake pad replacement")));

        JobCardMedia saved = service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, 55L, photo());

        assertThat(saved.getRepairTaskId()).isEqualTo(55L);
    }

    /**
     * A repair task's name is a display string. It may be arbitrary,
     * duplicated across jobs, renamed later, or empty. None of that may
     * affect whether media can be uploaded.
     */
    @Test
    void repairMedia_worksWhateverTheTaskIsCalled() {

        for (String name : List.of(
                "Brake pad replacement",
                "asdfgh",
                "  ",
                "Brake pad replacement")) {

            when(repairTaskRepository.findById(55L))
                    .thenReturn(Optional.of(repairTaskWithName(55L, name)));

            JobCardMedia saved = service.uploadMedia(
                    JOB_CARD_ID, MediaStage.DURING_REPAIR, 55L, photo());

            assertThat(saved.getRepairTaskId())
                    .as("task named '%s' must not affect upload", name)
                    .isEqualTo(55L);
        }
    }

    @Test
    void repairMedia_withANullTaskName_isAccepted() {

        when(repairTaskRepository.findById(55L))
                .thenReturn(Optional.of(repairTaskWithName(55L, null)));

        JobCardMedia saved = service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, 55L, photo());

        assertThat(saved.getRepairTaskId()).isEqualTo(55L);
    }

    @Test
    void repairMedia_withMultipleTasksOnTheJob_targetsTheRequestedOne() {

        when(repairTaskRepository.findById(56L))
                .thenReturn(Optional.of(repairTaskWithName(56L, "Oil change")));

        JobCardMedia saved = service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, 56L, photo());

        assertThat(saved.getRepairTaskId()).isEqualTo(56L);
    }

    // ---- Guards that must still hold ----

    @Test
    void aRepairTaskOnANonRepairStage_isStillRejected() {

        assertThatThrownBy(() -> service.uploadMedia(
                JOB_CARD_ID, MediaStage.BEFORE_SERVICE, 55L, photo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only be specified for DURING_REPAIR");
    }

    @Test
    void anEmptyFile_isRejected() {

        MultipartFile empty = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, null, empty))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMissingStage_isRejected() {

        assertThatThrownBy(() -> service.uploadMedia(
                JOB_CARD_ID, null, null, photo()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- Media is addressed by id, never by a display string ----

    @Test
    void mediaIsPersistedAgainstTheJobCardId_notItsNumber() {

        service.uploadMedia(JOB_CARD_ID, MediaStage.DURING_REPAIR, null, photo());

        ArgumentCaptor<JobCardMedia> captor = ArgumentCaptor.forClass(JobCardMedia.class);
        org.mockito.Mockito.verify(jobCardMediaRepository).save(captor.capture());

        assertThat(captor.getValue().getJobCardId()).isEqualTo(JOB_CARD_ID);
    }

    @Test
    void anAssignedTechnicianCanUploadRepairMedia() {

        authenticateAsTechnician();

        JobCardMedia saved = service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, null, photo());

        assertThat(saved.getUploadedBy()).isEqualTo(TECHNICIAN_ID);
    }

    // ---- Drive failures become actionable, not a bare 500 ----

    @Test
    void aDriveFailure_surfacesAsAMediaExceptionWithACode() throws Exception {

        when(googleDriveFileService.uploadFile(any(), anyString(), anyString()))
                .thenThrow(new IOException("connection reset"));

        assertThatThrownBy(() -> service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, null, photo()))
                .isInstanceOf(MediaException.class)
                .satisfies(thrown -> assertThat(((MediaException) thrown).getErrorCode())
                        .isEqualTo(MediaException.MediaErrorCode.MEDIA_DRIVE_UPLOAD_FAILED));
    }

    @Test
    void aDriveFailureMessage_neverLeaksTheUnderlyingGoogleError() {

        // Google's exceptions can carry request URLs and token metadata.
        try {
            when(googleDriveFileService.uploadFile(any(), anyString(), anyString()))
                    .thenThrow(new IOException("https://oauth2.googleapis.com/token refused"));
        } catch (Exception ignored) {
            // stubbing only
        }

        assertThatThrownBy(() -> service.uploadMedia(
                JOB_CARD_ID, MediaStage.DURING_REPAIR, null, photo()))
                .isInstanceOf(MediaException.class)
                .hasMessageNotContaining("oauth2.googleapis.com")
                .hasMessageNotContaining("token");
    }
}
