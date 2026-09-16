package com.garageos.modules.delivery.service.impl;

import com.garageos.core.enums.InvoiceStatus;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.enums.PaymentStatus;
import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.delivery.dto.request.CreateDeliveryRequest;
import com.garageos.modules.delivery.dto.response.DeliveryResponse;
import com.garageos.modules.delivery.mapper.DeliveryMapper;
import com.garageos.modules.delivery.repository.DeliveryRepository;
import com.garageos.modules.garage.entity.Garage;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.entity.Invoice;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.jobcard.validator.JobCardStatusValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
 * Canonical enforcement: invoice must be GENERATED and PAID before a
 * delivery can be recorded (existing semantics already treat Delivery
 * creation as the completed-delivery event), and that event moves the
 * JobCard to DELIVERED.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock private DeliveryRepository repository;
    @Mock private JobCardRepository jobCardRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private DeliveryMapper mapper;
    @Mock private JobCardStatusValidator statusValidator;

    @InjectMocks
    private DeliveryServiceImpl service;

    private static final Long JOB_CARD_ID = 500L;
    private static final Long INVOICE_ID = 800L;
    private static final Long GARAGE_ID = 10L;
    private static final Long OTHER_GARAGE_ID = 20L;

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

    private JobCard jobCard() {
        JobCard jobCard = new JobCard();
        jobCard.setId(JOB_CARD_ID);
        jobCard.setStatus(JobCardStatus.READY_FOR_DELIVERY);
        jobCard.setGarage(garage(GARAGE_ID));
        return jobCard;
    }

    private Invoice invoice(InvoiceStatus invoiceStatus, PaymentStatus paymentStatus) {
        Invoice invoice = new Invoice();
        invoice.setId(INVOICE_ID);
        invoice.setInvoiceStatus(invoiceStatus);
        invoice.setPaymentStatus(paymentStatus);
        return invoice;
    }

    private CreateDeliveryRequest request() {
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setJobCardId(JOB_CARD_ID);
        request.setInvoiceId(INVOICE_ID);
        return request;
    }

    @Test
    void unpaidInvoice_cannotCompleteDelivery() {

        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard()));
        when(invoiceRepository.findById(INVOICE_ID))
                .thenReturn(Optional.of(invoice(InvoiceStatus.GENERATED, PaymentStatus.PENDING)));

        authenticate(GARAGE_ID, "MANAGER");

        assertThatThrownBy(() -> service.createDelivery(request()))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void paidInvoice_canCompleteDelivery_andMovesJobCardToDelivered() {

        JobCard jobCard = jobCard();
        Invoice invoice = invoice(InvoiceStatus.GENERATED, PaymentStatus.PAID);

        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(mapper.toEntity(any())).thenReturn(new com.garageos.modules.delivery.entity.Delivery());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(DeliveryResponse.builder().build());

        authenticate(GARAGE_ID, "MANAGER");

        DeliveryResponse response = service.createDelivery(request());

        assertThat(response).isNotNull();
        assertThat(jobCard.getStatus()).isEqualTo(JobCardStatus.DELIVERED);
        verify(jobCardRepository).save(jobCard);
    }

    @Test
    void ungeneratedInvoice_cannotCompleteDelivery() {

        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard()));
        when(invoiceRepository.findById(INVOICE_ID))
                .thenReturn(Optional.of(invoice(InvoiceStatus.DRAFT, PaymentStatus.PENDING)));

        authenticate(GARAGE_ID, "MANAGER");

        assertThatThrownBy(() -> service.createDelivery(request()))
                .isInstanceOf(BusinessException.class);
    }

    // ---- Cross-garage tenant authorization (Defect #7 corrective fix) ----

    @Test
    void garageB_manager_completingGarageA_delivery_isDenied_withNoMutation() {

        JobCard jobCard = jobCard();
        Invoice invoice = invoice(InvoiceStatus.GENERATED, PaymentStatus.PAID);

        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));

        authenticate(OTHER_GARAGE_ID, "MANAGER");

        assertThatThrownBy(() -> service.createDelivery(request()))
                .isInstanceOf(BusinessException.class);

        verify(invoiceRepository, never()).findById(any());
        verify(repository, never()).save(any());
        verify(jobCardRepository, never()).save(any());
        assertThat(jobCard.getStatus()).isEqualTo(JobCardStatus.READY_FOR_DELIVERY);
    }

    @Test
    void garageA_serviceAdvisor_completingGarageA_delivery_isAllowed() {

        JobCard jobCard = jobCard();
        Invoice invoice = invoice(InvoiceStatus.GENERATED, PaymentStatus.PAID);

        when(jobCardRepository.findById(JOB_CARD_ID)).thenReturn(Optional.of(jobCard));
        when(invoiceRepository.findById(INVOICE_ID)).thenReturn(Optional.of(invoice));
        when(mapper.toEntity(any())).thenReturn(new com.garageos.modules.delivery.entity.Delivery());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(DeliveryResponse.builder().build());

        authenticate(GARAGE_ID, "SERVICE_ADVISOR");

        DeliveryResponse response = service.createDelivery(request());

        assertThat(response).isNotNull();
        assertThat(jobCard.getStatus()).isEqualTo(JobCardStatus.DELIVERED);
    }
}
