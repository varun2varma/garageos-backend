package com.garageos.modules.delivery.preference;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Returns/sets how the customer wants the completed vehicle returned (collection vs driver delivery). */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReturnPreferenceController {

    private final ReturnPreferenceService service;

    /** Owning customer, or staff of the job card garage. */
    @GetMapping("/job-cards/{jobCardId}/return-preference")
    public ResponseEntity<ApiResponse<ReturnPreferenceResponse>> get(@PathVariable Long jobCardId) {
        return ApiResponseUtil.success("Return preference fetched.", service.get(jobCardId));
    }

    /** Owning customer only. Idempotent; changeable until a driver is assigned. */
    @PutMapping("/job-cards/{jobCardId}/return-preference")
    public ResponseEntity<ApiResponse<ReturnPreferenceResponse>> set(
            @PathVariable Long jobCardId,
            @Valid @RequestBody SetReturnPreferenceRequest request) {
        return ApiResponseUtil.success("Return preference saved.", service.set(jobCardId, request));
    }

    /** Customer app addressing by job card number; same authorization as the id-based routes. */
    @GetMapping("/customer/jobcards/{jobCardNumber}/return-preference")
    public ResponseEntity<ApiResponse<ReturnPreferenceResponse>> getByNumber(@PathVariable String jobCardNumber) {
        return ApiResponseUtil.success("Return preference fetched.", service.getByNumber(jobCardNumber));
    }

    @PutMapping("/customer/jobcards/{jobCardNumber}/return-preference")
    public ResponseEntity<ApiResponse<ReturnPreferenceResponse>> setByNumber(
            @PathVariable String jobCardNumber,
            @Valid @RequestBody SetReturnPreferenceRequest request) {
        return ApiResponseUtil.success("Return preference saved.", service.setByNumber(jobCardNumber, request));
    }
}
