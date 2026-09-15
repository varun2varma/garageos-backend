package com.garageos.modules.media.controller;

import com.garageos.core.enums.media.MediaVisibility;
import com.garageos.modules.identity.security.jwt.JwtService;
import com.garageos.modules.identity.security.service.GarageUserDetailsService;
import com.garageos.modules.media.dto.response.JobCardMediaResponse;
import com.garageos.modules.media.entity.JobCardMedia;
import com.garageos.modules.media.mapper.JobCardMediaMapper;
import com.garageos.modules.media.service.MediaService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-boundary test for the one authorization rule that lives in an
 * annotation rather than in service code: {@code MediaController}'s
 * {@code @PreAuthorize(VISIBILITY_UPDATE_ROLES)} on the visibility-
 * correction endpoint. A plain Mockito unit test of {@code MediaService}
 * cannot exercise this (it never sees the annotation) — this uses a
 * {@code @WebMvcTest} slice with method security enabled but without the
 * production {@code SecurityConfig} (which pulls in JWT filter/beans this
 * repository has no test scaffolding for), matching this focused feature's
 * scope: no new heavyweight test infrastructure, just enough to prove the
 * role gate on this one endpoint.
 */
@WebMvcTest(controllers = MediaController.class)
@Import(MediaControllerVisibilityAuthorizationTest.MethodSecurityTestConfig.class)
class MediaControllerVisibilityAuthorizationTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    @MockitoBean
    private JobCardMediaMapper mediaMapper;

    // @WebMvcTest auto-includes any Filter bean, which pulls in the
    // production JwtAuthenticationFilter (it's a @Component) and its two
    // constructor dependencies — mocked here purely to satisfy that
    // construction, not exercised by any test below (WithMockUser bypasses
    // the filter chain entirely).
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private GarageUserDetailsService garageUserDetailsService;

    private static final String VALID_BODY = "{\"visibility\":\"INTERNAL\"}";

    private void stubSuccessfulUpdate() {
        JobCardMedia media = new JobCardMedia();
        media.setId(5L);
        when(mediaService.updateVisibility(anyLong(), anyLong(), any(MediaVisibility.class)))
                .thenReturn(media);
        when(mediaMapper.toResponse(media))
                .thenReturn(JobCardMediaResponse.builder().id(5L).visibility("INTERNAL").build());
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void owner_canChangeVisibility() throws Exception {
        stubSuccessfulUpdate();

        mockMvc.perform(put("/api/v1/job-cards/1/media/5/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void manager_canChangeVisibility() throws Exception {
        stubSuccessfulUpdate();

        mockMvc.perform(put("/api/v1/job-cards/1/media/5/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SERVICE_ADVISOR")
    void serviceAdvisor_canChangeVisibility() throws Exception {
        stubSuccessfulUpdate();

        mockMvc.perform(put("/api/v1/job-cards/1/media/5/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technician_isDenied() throws Exception {
        // NOT status().isForbidden(): this backend's GlobalExceptionHandler
        // has a catch-all @ExceptionHandler(Exception.class) that runs
        // inside the DispatcherServlet and intercepts the
        // AuthorizationDeniedException @PreAuthorize throws before Spring
        // Security's own translation ever gets a chance to turn it into a
        // 403 — so the ACTUAL, verified response is 500 "Something went
        // wrong.", not 403. This is a pre-existing backend behavior (not
        // introduced by this feature, and not specific to this endpoint —
        // it would affect MediaController.uploadMedia's pre-existing
        // @PreAuthorize identically), confirmed here rather than assumed.
        // Flagged in the completion report; intentionally NOT fixed, per
        // "do not change unrelated files" / "reuse existing architecture."
        // What IS proven and asserted below: mediaService.updateVisibility
        // is never invoked for a TECHNICIAN — the update never happens,
        // regardless of which status code reports that fact.
        mockMvc.perform(put("/api/v1/job-cards/1/media/5/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is5xxServerError());

        org.mockito.Mockito.verifyNoInteractions(mediaService);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_isDenied() throws Exception {
        // Same GlobalExceptionHandler caveat as technician_isDenied above.
        mockMvc.perform(put("/api/v1/job-cards/1/media/5/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is5xxServerError());

        org.mockito.Mockito.verifyNoInteractions(mediaService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void invalidVisibilityValue_isRejected() throws Exception {
        mockMvc.perform(put("/api/v1/job-cards/1/media/5/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .as("an unrecognized visibility value must not be accepted as 200 OK")
                            .isNotEqualTo(200);
                });
    }
}
