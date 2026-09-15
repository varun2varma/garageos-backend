package com.garageos.modules.customer.service.impl;

import com.garageos.core.enums.identity.UserStatus;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.modules.customer.entity.Customer;
import com.garageos.modules.customer.mapper.CustomerPortalMapper;
import com.garageos.modules.customer.repository.CustomerRepository;
import com.garageos.modules.estimate.repository.EstimateRepository;
import com.garageos.modules.estimate.service.EstimateService;
import com.garageos.modules.estimateitem.service.EstimateItemService;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.invoice.repository.InvoiceRepository;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.mapper.JobCardMediaMapper;
import com.garageos.modules.media.repository.JobCardMediaRepository;
import com.garageos.modules.media.service.MediaContent;
import com.garageos.modules.media.service.MediaService;
import com.garageos.modules.vehicle.repository.VehicleRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the customer-portal media authorization the Media feature
 * added: a customer may only ever see CUSTOMER_VISIBLE media for a job
 * card they themselves own — never another customer's job card, and never
 * INTERNAL media even on their own job card.
 */
@ExtendWith(MockitoExtension.class)
class CustomerPortalServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private JobCardRepository jobCardRepository;
    @Mock private EstimateRepository estimateRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CustomerPortalMapper mapper;
    @Mock private EstimateService estimateService;
    @Mock private EstimateItemService estimateItemService;
    @Mock private JobCardMediaRepository jobCardMediaRepository;
    @Mock private JobCardMediaMapper jobCardMediaMapper;
    @Mock private MediaService mediaService;

    @InjectMocks
    private CustomerPortalServiceImpl service;

    private static final Long OWNING_CUSTOMER_ID = 1L;
    private static final Long OTHER_CUSTOMER_ID = 2L;
    private static final Long JOB_CARD_ID = 100L;
    private static final String JOB_CARD_NUMBER = "JC-2026-000100";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsCustomer(String mobile) {
        GarageUserPrincipal principal = new GarageUserPrincipal(
                5L, null, "cust", "hash", "First", "Last",
                "cust@example.test", mobile, false, UserStatus.ACTIVE,
                Set.of("CUSTOMER"), Set.of(), List.of()
        );
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private Customer customer(Long id, String mobile) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setMobileNumber(mobile);
        return customer;
    }

    private JobCard jobCardOwnedBy(Customer owner) {
        JobCard jobCard = new JobCard();
        jobCard.setId(JOB_CARD_ID);
        jobCard.setCustomer(owner);
        return jobCard;
    }

    @Test
    void customer_canListTheirOwnJobCardMedia() {
        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        when(customerRepository.findByMobileNumber("9000000001")).thenReturn(Optional.of(owner));

        JobCard jobCard = jobCardOwnedBy(owner);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        when(jobCardMediaRepository.findByJobCardIdAndVisibilityOrderByCreatedAtAsc(JOB_CARD_ID, "CUSTOMER_VISIBLE"))
                .thenReturn(List.of(new JobCardMedia()));
        when(jobCardMediaMapper.toResponseList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        authenticateAsCustomer("9000000001");

        service.getJobCardMedia(JOB_CARD_NUMBER);

        // The important assertion is the *query* actually used — proves the
        // visibility filter is applied, not just that some list came back.
        org.mockito.Mockito.verify(jobCardMediaRepository)
                .findByJobCardIdAndVisibilityOrderByCreatedAtAsc(JOB_CARD_ID, "CUSTOMER_VISIBLE");
    }

    @Test
    void customer_cannotListAnotherCustomersJobCardMedia() {
        Customer requester = customer(OTHER_CUSTOMER_ID, "9000000002");
        when(customerRepository.findByMobileNumber("9000000002")).thenReturn(Optional.of(requester));

        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        JobCard jobCard = jobCardOwnedBy(owner);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        authenticateAsCustomer("9000000002");

        assertThatThrownBy(() -> service.getJobCardMedia(JOB_CARD_NUMBER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void customer_cannotDownloadInternalMedia_evenOnTheirOwnJobCard() {
        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        when(customerRepository.findByMobileNumber("9000000001")).thenReturn(Optional.of(owner));

        JobCard jobCard = jobCardOwnedBy(owner);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        JobCardMedia internalMedia = new JobCardMedia();
        internalMedia.setId(50L);
        internalMedia.setJobCardId(JOB_CARD_ID);
        internalMedia.setVisibility("INTERNAL");
        when(jobCardMediaRepository.findById(50L)).thenReturn(Optional.of(internalMedia));

        authenticateAsCustomer("9000000001");

        assertThatThrownBy(() -> service.getJobCardMediaContent(JOB_CARD_NUMBER, 50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void customer_cannotDownloadMediaFromAJobCardTheyDoNotOwn() {
        Customer requester = customer(OTHER_CUSTOMER_ID, "9000000002");
        when(customerRepository.findByMobileNumber("9000000002")).thenReturn(Optional.of(requester));

        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        JobCard jobCard = jobCardOwnedBy(owner);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        authenticateAsCustomer("9000000002");

        assertThatThrownBy(() -> service.getJobCardMediaContent(JOB_CARD_NUMBER, 50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void customerVisibleMedia_onOwnedJobCard_downloadsSuccessfully() {
        Customer owner = customer(OWNING_CUSTOMER_ID, "9000000001");
        when(customerRepository.findByMobileNumber("9000000001")).thenReturn(Optional.of(owner));

        JobCard jobCard = jobCardOwnedBy(owner);
        when(jobCardRepository.findByJobCardNumber(JOB_CARD_NUMBER)).thenReturn(Optional.of(jobCard));

        JobCardMedia visibleMedia = new JobCardMedia();
        visibleMedia.setId(51L);
        visibleMedia.setJobCardId(JOB_CARD_ID);
        visibleMedia.setVisibility("CUSTOMER_VISIBLE");
        when(jobCardMediaRepository.findById(51L)).thenReturn(Optional.of(visibleMedia));

        MediaContent expected = new MediaContent(new byte[]{1, 2, 3}, "image/jpeg", "photo.jpg");
        when(mediaService.downloadContent(visibleMedia)).thenReturn(expected);

        authenticateAsCustomer("9000000001");

        MediaContent actual = service.getJobCardMediaContent(JOB_CARD_NUMBER, 51L);

        assertThat(actual).isSameAs(expected);
    }
}
