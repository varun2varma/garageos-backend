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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobcards")
@RequiredArgsConstructor
public class JobCardController {

    private final JobCardService jobCardService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobCardResponse>> createJobCard(
            @Valid @RequestBody CreateJobCardRequest request) {

        JobCardResponse response = jobCardService.createJobCard(request);

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
                jobCardService.getJobCard(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobCardResponse>> updateJobCard(
            @PathVariable Long id,
            @Valid @RequestBody CreateJobCardRequest request) {

        return ApiResponseUtil.success(
                "Job Card updated successfully.",
                jobCardService.updateJobCard(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJobCard(
            @PathVariable Long id) {

        jobCardService.deleteJobCard(id);

        return ApiResponseUtil.success(
                "Job Card deleted successfully."
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<JobCardResponse>> getJobCardByNumber(
            @RequestParam String jobCardNumber) {

        return ApiResponseUtil.success(
                "Job Card fetched successfully.",
                jobCardService.getJobCardByNumber(jobCardNumber)
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
                jobCardService.getAllJobCards(
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

//    @PutMapping("/{jobCardNumber}/complete")
//    public ResponseEntity<ApiResponse<JobCardResponse>> completeJobCard(
//            @PathVariable String jobCardNumber) {
//
//        return ApiResponseUtil.success(
//                "Job Card marked as work completed.",
//                jobCardService.completeJobCard(jobCardNumber)
//        );
//    }

    @PutMapping("/{jobCardNumber}/ready-for-delivery")
    public ResponseEntity<ApiResponse<JobCardResponse>> readyForDelivery(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Job Card is ready for delivery.",
                jobCardService.readyForDelivery(jobCardNumber)
        );
    }

    @PutMapping("/{jobCardNumber}/close")
    public ResponseEntity<ApiResponse<JobCardResponse>> closeJobCard(
            @PathVariable String jobCardNumber) {

        return ApiResponseUtil.success(
                "Job Card closed successfully.",
                jobCardService.closeJobCard(jobCardNumber)
        );
    }
    @PostMapping("/{jobCardNumber}/inspection/start")
    public ResponseEntity<JobCardResponse> startInspection(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(jobCardService.startInspection(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/inspection/complete")
    public ResponseEntity<JobCardResponse> completeInspection(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(jobCardService.completeInspection(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/estimate/prepare")
    public ResponseEntity<JobCardResponse> prepareEstimate(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(jobCardService.prepareEstimate(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/estimate/approve")
    public ResponseEntity<JobCardResponse> approveEstimate(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(jobCardService.approveEstimate(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/repair/start")
    public ResponseEntity<JobCardResponse> startRepair(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(jobCardService.startRepair(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/quality-check")
    public ResponseEntity<JobCardResponse> qualityCheck(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(jobCardService.performQualityCheck(jobCardNumber));
    }
}