package com.garageos.modules.delivery.controller;

import com.garageos.modules.delivery.dto.response.DeliveryResponse;
import com.garageos.modules.delivery.service.DeliveryService;
import com.garageos.modules.identity.security.jwt.JwtService;
import com.garageos.modules.identity.security.service.GarageUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Backend hardening: DeliveryController.createDelivery() previously had no
 * @PreAuthorize at all - any authenticated role could invoke it (the
 * service-layer garage check added for Defect #7 still blocked cross-garage
 * callers, but same-garage low-privilege roles were unrestricted). Mirrors
 * MediaControllerVisibilityAuthorizationTest's @WebMvcTest pattern, the
 * only existing controller-boundary @PreAuthorize test in this repo.
 */
@WebMvcTest(controllers = DeliveryController.class)
@Import(DeliveryControllerAuthorizationTest.MethodSecurityTestConfig.class)
class DeliveryControllerAuthorizationTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryService deliveryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private GarageUserDetailsService garageUserDetailsService;

    private static final String VALID_BODY =
            "{\"jobCardId\":1,\"invoiceId\":1,\"deliveredBy\":\"Advisor\",\"receivedBy\":\"Customer\"}";

    private void stubSuccessfulCreate() {
        when(deliveryService.createDelivery(any()))
                .thenReturn(DeliveryResponse.builder().id(1L).build());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void owner_canCreateDelivery() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void manager_canCreateDelivery() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SERVICE_ADVISOR")
    void serviceAdvisor_canCreateDelivery() throws Exception {
        stubSuccessfulCreate();

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technician_isDenied_noMutation() throws Exception {
        // Same GlobalExceptionHandler caveat documented in
        // MediaControllerVisibilityAuthorizationTest: the catch-all
        // @ExceptionHandler(Exception.class) intercepts the
        // AuthorizationDeniedException before Spring Security's own
        // translation produces a 403, so the actual response is 500 -
        // this is the pre-existing, out-of-scope Defect #4, not
        // introduced here. What matters for this hardening item is that
        // the delivery is never created.
        mockMvc.perform(post("/api/v1/deliveries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(deliveryService);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_isDenied_noMutation() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(deliveryService);
    }
}
