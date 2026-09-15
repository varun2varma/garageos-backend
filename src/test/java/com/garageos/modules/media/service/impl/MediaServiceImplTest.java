package com.garageos.modules.media.service.impl;

import com.garageos.core.enums.JobAssignmentStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.enums.media.MediaVisibility;
import com.garageos.core.exception.ResourceNotFoundException;
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
import com.garageos.modules.repairtask.repository.RepairTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the authorization rules the Media feature added to
 * {@link MediaServiceImpl}: garage isolation (pre-existing, re-verified
 * here) and the new technician-assignment check, exercised through the
 * public {@code listMedia} method (which shares the same
 * {@code authorizeEmployeeAccess} path as upload and content download,
 * without needing to also mock Google Drive).
 *
 * No Spring context / database is started — this repository has no test
 * infrastructure (H2/Testcontainers) set up for that, and adding one is
 * outside this focused feature's scope. These are plain Mockito unit
 * tests of the service's own logic.
 */
@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private JobCardRepository jobCardRepository;

    @Mock
    private JobCardMediaRepository jobCardMediaRepository;

    @Mock
    private RepairTaskRepository repairTaskRepository;

    @Mock
    private JobAssignmentRepository jobAssignmentRepository;

    @Mock
    private GoogleDriveFolderService folderService;

    @Mock
    private GoogleDriveFileService googleDriveFileService;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private static final Long GARAGE_A = 1L;
    private static final Long GARAGE_B = 2L;
    private static final Long JOB_CARD_ID = 100L;
    private static final Long TECHNICIAN_USER_ID = 10L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, Long garageId, String... roles) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                userId, garageId, "user" + userId, "hash",
                "First", "Last", "first@example.test", "9999999999",
                false, UserStatus.ACTIVE, Set.of(roles), Set.of(), List.of()
        );
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private JobCard jobCardInGarage(Long garageId) {
        Garage garage = new Garage();
        garage.setId(garageId);

        JobCard jobCard = new JobCard();
        jobCard.setId(JOB_CARD_ID);
        jobCard.setGarage(garage);
        return jobCard;
    }

    @Test
    void managerInSameGarage_canListMedia_noTechnicianCheckNeeded() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));
        when(jobCardMediaRepository.findByJobCardIdOrderByCreatedAtAsc(JOB_CARD_ID))
                .thenReturn(List.of(new JobCardMedia()));

        authenticateAs(1L, GARAGE_A, "MANAGER");

        List<JobCardMedia> result = mediaService.listMedia(JOB_CARD_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void ownerInSameGarage_canListMedia() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));
        when(jobCardMediaRepository.findByJobCardIdOrderByCreatedAtAsc(JOB_CARD_ID))
                .thenReturn(List.of());

        authenticateAs(1L, GARAGE_A, "OWNER");

        assertThat(mediaService.listMedia(JOB_CARD_ID)).isEmpty();
    }

    @Test
    void userFromDifferentGarage_isDenied_evenIfManager() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        authenticateAs(1L, GARAGE_B, "MANAGER");

        assertThatThrownBy(() -> mediaService.listMedia(JOB_CARD_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void technicianWithNonCancelledAssignment_canListMedia() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));
        when(jobCardMediaRepository.findByJobCardIdOrderByCreatedAtAsc(JOB_CARD_ID))
                .thenReturn(List.of());

        User technicianUser = new User();
        technicianUser.setId(TECHNICIAN_USER_ID);

        JobAssignment assignment = new JobAssignment();
        assignment.setUser(technicianUser);
        assignment.setStatus(JobAssignmentStatus.IN_PROGRESS);

        when(jobAssignmentRepository.findByJobCardId(JOB_CARD_ID))
                .thenReturn(List.of(assignment));

        authenticateAs(TECHNICIAN_USER_ID, GARAGE_A, "TECHNICIAN");

        assertThat(mediaService.listMedia(JOB_CARD_ID)).isEmpty();
    }

    @Test
    void technicianWithOnlyCancelledAssignment_isDenied() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        User technicianUser = new User();
        technicianUser.setId(TECHNICIAN_USER_ID);

        JobAssignment cancelled = new JobAssignment();
        cancelled.setUser(technicianUser);
        cancelled.setStatus(JobAssignmentStatus.CANCELLED);

        when(jobAssignmentRepository.findByJobCardId(JOB_CARD_ID))
                .thenReturn(List.of(cancelled));

        authenticateAs(TECHNICIAN_USER_ID, GARAGE_A, "TECHNICIAN");

        assertThatThrownBy(() -> mediaService.listMedia(JOB_CARD_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void technicianWithNoAssignmentAtAll_isDenied() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));
        when(jobAssignmentRepository.findByJobCardId(JOB_CARD_ID)).thenReturn(List.of());

        authenticateAs(TECHNICIAN_USER_ID, GARAGE_A, "TECHNICIAN");

        assertThatThrownBy(() -> mediaService.listMedia(JOB_CARD_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void roleWithNoMediaEntitlement_isDenied() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        authenticateAs(1L, GARAGE_A, "CASHIER");

        assertThatThrownBy(() -> mediaService.listMedia(JOB_CARD_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void nonexistentJobCard_isRejected() {
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.listMedia(JOB_CARD_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMediaContent_rejectsMediaBelongingToADifferentJobCard() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        JobCardMedia mediaFromAnotherJobCard = new JobCardMedia();
        mediaFromAnotherJobCard.setId(999L);
        mediaFromAnotherJobCard.setJobCardId(JOB_CARD_ID + 1);

        when(jobCardMediaRepository.findById(999L))
                .thenReturn(Optional.of(mediaFromAnotherJobCard));

        authenticateAs(1L, GARAGE_A, "MANAGER");

        assertThatThrownBy(() -> mediaService.getMediaContent(JOB_CARD_ID, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // Visibility-correction service logic (garage isolation +
    // job-card/media matching). Role gating itself (MANAGER/SERVICE_
    // ADVISOR/OWNER allowed, TECHNICIAN/CUSTOMER denied) lives entirely in
    // MediaController's @PreAuthorize, not in this service, so it is
    // covered separately in MediaControllerVisibilityAuthorizationTest.
    // ------------------------------------------------------------------

    @Test
    void updateVisibility_ownerInSameGarage_succeeds() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        JobCardMedia media = new JobCardMedia();
        media.setId(5L);
        media.setJobCardId(JOB_CARD_ID);
        media.setVisibility("CUSTOMER_VISIBLE");
        when(jobCardMediaRepository.findById(5L)).thenReturn(Optional.of(media));
        when(jobCardMediaRepository.save(media)).thenReturn(media);

        authenticateAs(1L, GARAGE_A, "OWNER");

        JobCardMedia result = mediaService.updateVisibility(JOB_CARD_ID, 5L, MediaVisibility.INTERNAL);

        assertThat(result.getVisibility()).isEqualTo("INTERNAL");
    }

    @Test
    void updateVisibility_wrongGarage_isDenied() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        authenticateAs(1L, GARAGE_B, "MANAGER");

        assertThatThrownBy(() -> mediaService.updateVisibility(JOB_CARD_ID, 5L, MediaVisibility.INTERNAL))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateVisibility_mediaBelongsToADifferentJobCard_isRejected() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        JobCardMedia mediaFromAnotherJobCard = new JobCardMedia();
        mediaFromAnotherJobCard.setId(6L);
        mediaFromAnotherJobCard.setJobCardId(JOB_CARD_ID + 1);
        when(jobCardMediaRepository.findById(6L)).thenReturn(Optional.of(mediaFromAnotherJobCard));

        authenticateAs(1L, GARAGE_A, "MANAGER");

        assertThatThrownBy(() -> mediaService.updateVisibility(JOB_CARD_ID, 6L, MediaVisibility.CUSTOMER_VISIBLE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateVisibility_doesNotTouchOtherFields() {
        JobCard jobCard = jobCardInGarage(GARAGE_A);
        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        JobCardMedia media = new JobCardMedia();
        media.setId(7L);
        media.setJobCardId(JOB_CARD_ID);
        media.setFileName("original.jpg");
        media.setMediaStage("AFTER_REPAIR");
        media.setVisibility("CUSTOMER_VISIBLE");
        when(jobCardMediaRepository.findById(7L)).thenReturn(Optional.of(media));
        when(jobCardMediaRepository.save(media)).thenReturn(media);

        authenticateAs(1L, GARAGE_A, "SERVICE_ADVISOR");

        JobCardMedia result = mediaService.updateVisibility(JOB_CARD_ID, 7L, MediaVisibility.INTERNAL);

        assertThat(result.getVisibility()).isEqualTo("INTERNAL");
        assertThat(result.getFileName()).isEqualTo("original.jpg");
        assertThat(result.getMediaStage()).isEqualTo("AFTER_REPAIR");
    }
}
