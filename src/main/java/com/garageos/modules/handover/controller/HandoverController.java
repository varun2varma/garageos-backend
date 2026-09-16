package com.garageos.modules.handover.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.handover.dto.request.VerifyHandoverRequest;
import com.garageos.modules.handover.dto.response.HandoverCodeResponse;
import com.garageos.modules.handover.dto.response.HandoverStatusResponse;
import com.garageos.modules.handover.service.HandoverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/navigation/trips/{tripId}/handover")
@RequiredArgsConstructor
public class HandoverController {

    private final HandoverService handoverService;

    /** Customer only: get (or freshly issue) the active confirmation code for their trip. */
    @GetMapping("/code")
    public ResponseEntity<ApiResponse<HandoverCodeResponse>> getCode(@PathVariable Long tripId) {

        return ApiResponseUtil.success(
                "Confirmation code issued.",
                handoverService.getOrCreateActiveCode(tripId)
        );
    }

    /** Driver only, for their own assigned trip: verify the code the customer read out. */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<HandoverStatusResponse>> verify(
            @PathVariable Long tripId,
            @Valid @RequestBody VerifyHandoverRequest request) {

        return ApiResponseUtil.success(
                "Handover verified.",
                handoverService.verify(tripId, request.getCode())
        );
    }

    /** Garage-scoped operational visibility (Manager/Advisor/Owner) - never returns the code. */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<HandoverStatusResponse>> getStatus(@PathVariable Long tripId) {

        return ApiResponseUtil.success(
                "Handover status fetched.",
                handoverService.getStatus(tripId)
        );
    }
}
