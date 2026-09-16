package com.garageos.modules.qualitycheck.service.impl;

import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.QualityCheckStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.qualitycheck.dto.request.CreateQualityCheckRequest;
import com.garageos.modules.qualitycheck.entity.QualityCheck;
import com.garageos.modules.qualitycheck.mapper.QualityCheckMapper;
import com.garageos.modules.qualitycheck.repository.QualityCheckRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the Defect #6 corrective fix: passQualityCheck()/failQualityCheck()
 * previously relied solely on @PreAuthorize(JOBCARD_OPERATIONAL_ROLES) at
 * the controller (role-only, no tenant scoping), so a MANAGER/OWNER/
 * SERVICE_ADVISOR from ANY garage could pass or fail QC on ANY garage's
 * JobCard - confirmed live against Postgres (Garage B Manager passed QC
 * on a Garage A JobCard). Mirrors RepairTaskServiceImplTest's pattern.
 */
@ExtendWith(MockitoExtension.class)
class QualityCheckServiceImplTest {

    @Mock private QualityCheckRepository repository;
    @Mock private JobCardRepository jobCardRepository;
    @Mock private QualityCheckMapper mapper;

    private QualityCheckServiceImpl service() {
        return new QualityCheckServiceImpl(repository, jobCardRepository, mapper);
    }

    private static final Long GARAGE_ID = 10L;
    private static final Long OTHER_GARAGE_ID = 20L;
    private static final String JC_NUMBER = "G006-JC-2026-000099";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long garageId, String role) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                999L, garageId, "user999", "hash", "First", "Last",
                "user@example.test", "9999999999", false, UserStatus.ACTIVE,
                Set.of(role), Set.of(), List.of()
        );
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Garage garage(Long id) {
        Garage g = new Garage();
        g.setId(id);
        return g;
    }

    private QualityCheck qualityCheck(JobCardStatus jobCardStatus) {
        JobCard jobCard = new JobCard();
        jobCard.setId(500L);
        jobCard.setJobCardNumber(JC_NUMBER);
        jobCard.setStatus(jobCardStatus);
        jobCard.setGarage(garage(GARAGE_ID));

        return QualityCheck.builder()
                .jobCard(jobCard)
                .status(QualityCheckStatus.PENDING)
                .build();
    }

    private CreateQualityCheckRequest request() {
        return CreateQualityCheckRequest.builder()
                .inspectedBy("Inspector")
                .remarks("ok")
                .build();
    }

    // ---- PASS ----

    @Test
    void garageA_manager_passQC_onGarageA_jobCard_isAllowed() {

        QualityCheck qc = qualityCheck(JobCardStatus.REPAIR_COMPLETED);

        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(qc));
        when(repository.save(any())).thenReturn(qc);
        when(jobCardRepository.save(any())).thenReturn(qc.getJobCard());
        when(mapper.toResponse(any())).thenReturn(null);

        authenticate(GARAGE_ID, "MANAGER");

        service().passQualityCheck(JC_NUMBER, request());

        assertThat(qc.getStatus()).isEqualTo(QualityCheckStatus.PASSED);
        assertThat(qc.getJobCard().getStatus()).isEqualTo(JobCardStatus.READY_FOR_INVOICE);
    }

    @Test
    void garageB_manager_passQC_onGarageA_jobCard_isDenied_withNoMutation() {

        QualityCheck qc = qualityCheck(JobCardStatus.REPAIR_COMPLETED);

        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(qc));

        authenticate(OTHER_GARAGE_ID, "MANAGER");

        assertThatThrownBy(() -> service().passQualityCheck(JC_NUMBER, request()))
                .isInstanceOf(BusinessException.class);

        assertThat(qc.getStatus()).isEqualTo(QualityCheckStatus.PENDING);
        assertThat(qc.getInspectedBy()).isNull();
        assertThat(qc.getJobCard().getStatus()).isEqualTo(JobCardStatus.REPAIR_COMPLETED);
        verify(repository, never()).save(any());
        verify(jobCardRepository, never()).save(any());
    }

    @Test
    void garageA_owner_passQC_onGarageA_jobCard_isAllowed() {

        QualityCheck qc = qualityCheck(JobCardStatus.REPAIR_COMPLETED);

        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(qc));
        when(repository.save(any())).thenReturn(qc);
        when(jobCardRepository.save(any())).thenReturn(qc.getJobCard());
        when(mapper.toResponse(any())).thenReturn(null);

        authenticate(GARAGE_ID, "OWNER");

        service().passQualityCheck(JC_NUMBER, request());

        assertThat(qc.getStatus()).isEqualTo(QualityCheckStatus.PASSED);
    }

    @Test
    void garageB_owner_passQC_onGarageA_jobCard_isDenied() {

        QualityCheck qc = qualityCheck(JobCardStatus.REPAIR_COMPLETED);

        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(qc));

        authenticate(OTHER_GARAGE_ID, "OWNER");

        assertThatThrownBy(() -> service().passQualityCheck(JC_NUMBER, request()))
                .isInstanceOf(BusinessException.class);

        assertThat(qc.getStatus()).isEqualTo(QualityCheckStatus.PENDING);
        verify(repository, never()).save(any());
    }

    @Test
    void garageA_serviceAdvisor_passQC_onGarageA_jobCard_isAllowed() {

        QualityCheck qc = qualityCheck(JobCardStatus.REPAIR_COMPLETED);

        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(qc));
        when(repository.save(any())).thenReturn(qc);
        when(jobCardRepository.save(any())).thenReturn(qc.getJobCard());
        when(mapper.toResponse(any())).thenReturn(null);

        authenticate(GARAGE_ID, "SERVICE_ADVISOR");

        service().passQualityCheck(JC_NUMBER, request());

        assertThat(qc.getStatus()).isEqualTo(QualityCheckStatus.PASSED);
    }

    @Test
    void garageB_serviceAdvisor_passQC_onGarageA_jobCard_isDenied_withNoMutation() {

        QualityCheck qc = qualityCheck(JobCardStatus.REPAIR_COMPLETED);

        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(qc));

        authenticate(OTHER_GARAGE_ID, "SERVICE_ADVISOR");

        assertThatThrownBy(() -> service().passQualityCheck(JC_NUMBER, request()))
                .isInstanceOf(BusinessException.class);

        assertThat(qc.getStatus()).isEqualTo(QualityCheckStatus.PENDING);
        assertThat(qc.getJobCard().getStatus()).isEqualTo(JobCardStatus.REPAIR_COMPLETED);
        verify(repository, never()).save(any());
        verify(jobCardRepository, never()).save(any());
    }

    // ---- 404 root cause (the traced "Quality Check not found." bug) ----

    @Test
    void noQualityCheckRecordExists_surfacesTheStableCode_notARawException() {

        // Reproduces the exact reported bug: a JobCard whose RepairTask(s)
        // were never actually completed via the canonical backend call
        // never gets a QualityCheck row created (RepairTaskServiceImpl.
        // completeRepair is the only thing that creates one), so passing
        // QC 404s here. This asserts the response carries the stable
        // QUALITY_CHECK_NOT_AVAILABLE code (Phase W) so Flutter can show a
        // specific message instead of a raw 404 body.
        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.empty());

        authenticate(GARAGE_ID, "MANAGER");

        assertThatThrownBy(() -> service().passQualityCheck(JC_NUMBER, request()))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(e -> assertThat(((ResourceNotFoundException) e).getCode())
                        .isEqualTo("QUALITY_CHECK_NOT_AVAILABLE"));

        verify(repository, never()).save(any());
    }

    @Test
    void getQualityCheck_noRecordExists_alsoSurfacesTheStableCode() {

        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getQualityCheck(JC_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class)
                .satisfies(e -> assertThat(((ResourceNotFoundException) e).getCode())
                        .isEqualTo("QUALITY_CHECK_NOT_AVAILABLE"));
    }

    // ---- FAIL ----

    @Test
    void garageA_authorizedEmployee_failQC_isAllowed() {

        QualityCheck qc = qualityCheck(JobCardStatus.REPAIR_COMPLETED);

        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(qc));
        when(repository.save(any())).thenReturn(qc);
        when(jobCardRepository.save(any())).thenReturn(qc.getJobCard());
        when(mapper.toResponse(any())).thenReturn(null);

        authenticate(GARAGE_ID, "MANAGER");

        service().failQualityCheck(JC_NUMBER, request());

        assertThat(qc.getStatus()).isEqualTo(QualityCheckStatus.FAILED);
        assertThat(qc.getJobCard().getStatus()).isEqualTo(JobCardStatus.REPAIR_PENDING);
    }

    @Test
    void garageB_employee_failQC_onGarageA_isDenied_withNoMutation() {

        QualityCheck qc = qualityCheck(JobCardStatus.REPAIR_COMPLETED);

        when(repository.findByJobCardJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(qc));

        authenticate(OTHER_GARAGE_ID, "MANAGER");

        assertThatThrownBy(() -> service().failQualityCheck(JC_NUMBER, request()))
                .isInstanceOf(BusinessException.class);

        assertThat(qc.getStatus()).isEqualTo(QualityCheckStatus.PENDING);
        assertThat(qc.getJobCard().getStatus()).isEqualTo(JobCardStatus.REPAIR_COMPLETED);
        verify(repository, never()).save(any());
        verify(jobCardRepository, never()).save(any());
    }
}
