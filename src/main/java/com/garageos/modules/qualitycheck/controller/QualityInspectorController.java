package com.garageos.modules.qualitycheck.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.qualitycheck.service.QualityInspectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The employees of the caller garage who may be recorded as a Quality Check inspector (never customers). */
@RestController
@RequestMapping("/api/v1/quality-inspectors")
@RequiredArgsConstructor
public class QualityInspectorController {

    private final QualityInspectorService service;
    private final GarageScope garageScope;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'SERVICE_ADVISOR', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<QualityInspectorService.Option>>> eligibleInspectors() {

        GarageUserPrincipal principal = garageScope.principal();

        return ApiResponseUtil.success(
                "Eligible inspectors fetched successfully.",
                service.listEligible(principal.getGarageId()));
    }
}
