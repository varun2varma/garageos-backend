package com.garageos.modules.jobcard.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.service.JobCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobcards")
@RequiredArgsConstructor
public class JobCardController {

    private final JobCardService service;

    @PostMapping
    public ResponseEntity<ApiResponse<JobCardResponse>> createJobCard(
            @Valid @RequestBody CreateJobCardRequest request) {

        JobCardResponse response = service.createJobCard(request);

        return ApiResponseUtil.created(
                "Job Card created successfully.",
                response
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobCardResponse>> getJobCard(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Job Card fetched successfully.",
                service.getJobCard(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobCardResponse>> updateJobCard(
            @PathVariable Long id,
            @Valid @RequestBody CreateJobCardRequest request) {

        return ApiResponseUtil.success(
                "Job Card updated successfully.",
                service.updateJobCard(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJobCard(
            @PathVariable Long id) {

        service.deleteJobCard(id);

        return ApiResponseUtil.success(
                "Job Card deleted successfully."
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<JobCardResponse>> getJobCardByNumber(
            @RequestParam String jobCardNumber) {

        return ApiResponseUtil.success(
                "Job Card fetched successfully.",
                service.getJobCardByNumber(jobCardNumber)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobCardResponse>>> getAllJobCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ApiResponseUtil.success(
                "Job Cards fetched successfully.",
                service.getAllJobCards(
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<JobCardResponse>> completeJobCard(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Job Card marked as work completed.",
                service.completeJobCard(id)
        );
    }

    @PutMapping("/{id}/ready-for-delivery")
    public ResponseEntity<ApiResponse<JobCardResponse>> readyForDelivery(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Job Card is ready for delivery.",
                service.readyForDelivery(id)
        );
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<JobCardResponse>> closeJobCard(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Job Card closed successfully.",
                service.closeJobCard(id)
        );
    }
}