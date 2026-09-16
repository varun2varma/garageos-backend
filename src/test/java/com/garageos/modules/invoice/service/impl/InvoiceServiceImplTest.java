package com.garageos.modules.invoice.service.impl;

import com.garageos.core.enums.EstimateStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.estimate.entity.Estimate;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimateitem.repository.EstimateItemRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.mapper.InvoiceMapper;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.invoiceitem.repository.InvoiceItemRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
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
 * Covers the Defect #7 corrective fix: generateInvoice()/receivePayment()
 * relied solely on @PreAuthorize(WORKFLOW_OPERATIONAL_ROLES) at the
 * controller (role-only, no tenant scoping), so a MANAGER/OWNER/
 * SERVICE_ADVISOR from ANY garage could generate an invoice or record
 * payment for ANY garage's JobCard - confirmed live against Postgres.
 * Uses the real JobCardStatusValidator so the JobCard transition is
 * exercised against the actual canonical table, not an assumption.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private EstimateRepository estimateRepository;
    @Mock private InvoiceMapper invoiceMapper;
    @Mock private JobCardRepository jobCardRepository;
    @Mock private InvoiceItemRepository invoiceItemRepository;
    @Mock private EstimateItemRepository estimateItemRepository;
    @Mock private JobCardService jobCardService;

    private final JobCardStatusValidator statusValidator = new JobCardStatusValidator();

    private InvoiceServiceImpl service() {
        return new InvoiceServiceImpl(
                invoiceRepository,
                estimateRepository,
                invoiceMapper,
                jobCardRepository,
                invoiceItemRepository,
                estimateItemRepository,
                statusValidator,
                jobCardService
        );
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

    private JobCard jobCard(JobCardStatus status) {
        JobCard jc = new JobCard();
        jc.setId(500L);
        jc.setJobCardNumber(JC_NUMBER);
        jc.setStatus(status);
        jc.setGarage(garage(GARAGE_ID));
        return jc;
    }

    private Estimate approvedEstimate(JobCard jobCard) {
        Estimate e = new Estimate();
        e.setId(700L);
        e.setStatus(EstimateStatus.APPROVED);
        e.setJobCard(jobCard);
        return e;
    }

    // ---- generateInvoice ----

    @Test
    void garageA_manager_generateInvoice_onGarageA_jobCard_isAllowed() {

        JobCard jc = jobCard(JobCardStatus.READY_FOR_INVOICE);
        Estimate estimate = approvedEstimate(jc);

        when(jobCardRepository.findByJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(jc));
        when(estimateRepository.findByJobCardId(jc.getId())).thenReturn(Optional.of(estimate));
        when(invoiceRepository.existsByEstimateId(estimate.getId())).thenReturn(false);
        when(invoiceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceMapper.toResponse(any())).thenReturn(null);

        authenticate(GARAGE_ID, "MANAGER");

        service().generateInvoice(JC_NUMBER);

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.INVOICE_GENERATED);
        verify(jobCardRepository).save(jc);
    }

    @Test
    void garageB_manager_generateInvoice_onGarageA_jobCard_isDenied_withNoMutation() {

        JobCard jc = jobCard(JobCardStatus.READY_FOR_INVOICE);

        when(jobCardRepository.findByJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(jc));

        authenticate(OTHER_GARAGE_ID, "MANAGER");

        assertThatThrownBy(() -> service().generateInvoice(JC_NUMBER))
                .isInstanceOf(BusinessException.class);

        assertThat(jc.getStatus()).isEqualTo(JobCardStatus.READY_FOR_INVOICE);
        verifyNoInteractions(estimateRepository, invoiceRepository);
        verify(jobCardRepository, never()).save(any());
    }

    // ---- receivePayment ----

    @Test
    void garageA_manager_receivePayment_onGarageA_jobCard_isAllowed() {

        JobCard jc = jobCard(JobCardStatus.INVOICE_GENERATED);

        Invoice invoice = new Invoice();
        invoice.setId(900L);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        when(jobCardRepository.findByJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(jc));
        when(invoiceRepository.findByEstimateJobCardId(jc.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceMapper.toResponse(any())).thenReturn(null);

        authenticate(GARAGE_ID, "MANAGER");

        service().receivePayment(JC_NUMBER);

        assertThat(invoice.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(jobCardService).readyForDelivery(JC_NUMBER);
    }

    @Test
    void garageB_manager_receivePayment_onGarageA_jobCard_isDenied_withNoMutation() {

        JobCard jc = jobCard(JobCardStatus.INVOICE_GENERATED);

        when(jobCardRepository.findByJobCardNumber(JC_NUMBER)).thenReturn(Optional.of(jc));

        authenticate(OTHER_GARAGE_ID, "MANAGER");

        assertThatThrownBy(() -> service().receivePayment(JC_NUMBER))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(invoiceRepository, jobCardService);
    }
}
