package com.garageos.modules.owner.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.modules.owner.dto.response.OwnerDashboardSummaryResponse;
import com.garageos.modules.owner.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner/dashboard")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;

    @GetMapping("/summary")
    public ApiResponse<OwnerDashboardSummaryResponse> getSummary(

            @RequestParam(required = false)
            Long garageId,

            @RequestParam(defaultValue = "false")
            boolean allGarages,

            @RequestParam(required = false)
            LocalDate from,

            @RequestParam(required = false)
            LocalDate to
    ) {

        OwnerDashboardSummaryResponse response =
                ownerDashboardService.getSummary(
                        garageId,
                        allGarages,
                        from,
                        to
                );

        return ApiResponse
                .<OwnerDashboardSummaryResponse>builder()
                .success(true)
                .message(
                        "Owner dashboard summary fetched successfully."
                )
                .data(response)
                .timestamp(LocalDateTime.now())
                .requestId(UUID.randomUUID().toString())
                .build();
    }
}