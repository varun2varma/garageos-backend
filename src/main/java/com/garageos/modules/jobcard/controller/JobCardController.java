package com.garageos.modules.jobcard.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.enums.JobCardStatus;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.jobcard.dto.request.CreateJobCardRequest;
import com.garageos.modules.jobcard.dto.response.JobCardResponse;
import com.garageos.modules.jobcard.dto.response.JobCardViewResponse;
import com.garageos.modules.jobcard.service.JobCardProjectionService;
import com.garageos.modules.jobcard.service.JobCardService;
import com.garageos.modules.qualitycheck.dto.request.CreateQualityCheckRequest;
import com.garageos.modules.qualitycheck.dto.response.QualityCheckResponse;
import com.garageos.modules.qualitycheck.service.QualityCheckService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobcards")
@RequiredArgsConstructor
public class JobCardController {

    private final JobCardService jobCardService;
    private final QualityCheckService qualityCheckService;
    private final JobCardProjectionService projectionService;

    /**
     * Locked operational-access decision: OWNER/MANAGER/SERVICE_ADVISOR
     * for general operational JobCard actions on this controller.
     * estimate/approve is deliberately NOT annotated here — see the
     * identical note in ServiceWorkflowController.
     */
    private static final String JOBCARD_OPERATIONAL_ROLES = """
            hasAnyRole(
                'MANAGER',
                'SERVICE_ADVISOR',
                'OWNER'
            )
            """;

    @GetMapping("/{id}/view")
    @Operation(summary = "Canonical role-aware Job Card projection (read-only)")
    public ResponseEntity<ApiResponse<JobCardViewResponse>> getJobCardView(
            @PathVariable Long id) {

        return ApiResponseUtil.success(
                "Job Card view fetched successfully.",
                projectionService.getJobCardView(id)
        );
    }

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

    /**
     * garageId (optional, validated) and allGarages let a multi-garage
     * Owner view a specific other garage or every garage they belong to;
     * every other caller gets their own garage's Job Cards exactly as
     * before these parameters existed.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobCardResponse>>> getAllJobCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Long garageId,
            @RequestParam(defaultValue = "false") boolean allGarages,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String stage) {

        return ApiResponseUtil.success(
                "Job Cards fetched successfully.",
                jobCardService.getAllJobCards(
                        page,
                        size,
                        sortBy,
                        direction,
                        garageId,
                        allGarages,
                        resolveStatusFilter(status, stage)
                )
        );
    }

    /**
     * Two ways to narrow the list, both optional and both resolved here
     * so the service only ever sees canonical {@link JobCardStatus}
     * values:
     *
     * <ul>
     *   <li>{@code status} - explicit status names, for a caller that
     *       already knows exactly which states it wants.</li>
     *   <li>{@code stage} - a named operational bucket the Job list's
     *       filter chips use ("inspection", "approval", "repair", "qc",
     *       "invoice", "delivery", "attention", "active"). Several
     *       canonical states map to one bucket, and legacy non-canonical
     *       states are folded into the bucket a job in that state would
     *       actually be sitting in, so an older Job Card is not silently
     *       missing from the operational view.</li>
     * </ul>
     *
     * An unrecognised value is ignored rather than rejected, matching how
     * the rest of this API treats unknown enum strings; {@code status}
     * wins when both are supplied.
     */
    private List<JobCardStatus> resolveStatusFilter(
            List<String> status,
            String stage) {

        if (status != null && !status.isEmpty()) {

            return status.stream()
                    .map(this::parseStatusOrNull)
                    .filter(Objects::nonNull)
                    .toList();
        }

        if (stage == null || stage.isBlank()) {
            return List.of();
        }

        return switch (stage.trim().toLowerCase(Locale.ROOT)) {

            case "inspection" -> List.of(
                    JobCardStatus.OPEN,
                    JobCardStatus.INSPECTION_PENDING);

            case "estimate" -> List.of(
                    JobCardStatus.INSPECTION_COMPLETED,
                    JobCardStatus.ESTIMATE_PENDING);

            case "approval" -> List.of(
                    JobCardStatus.WAITING_FOR_APPROVAL);

            case "repair" -> List.of(
                    JobCardStatus.ESTIMATE_APPROVED,
                    JobCardStatus.REPAIR_PENDING,
                    JobCardStatus.REPAIR_IN_PROGRESS);

            case "qc" -> List.of(
                    JobCardStatus.REPAIR_COMPLETED,
                    JobCardStatus.QUALITY_CHECK);

            case "invoice" -> List.of(
                    JobCardStatus.READY_FOR_INVOICE,
                    JobCardStatus.INVOICE_GENERATED,
                    JobCardStatus.INVOICED,
                    JobCardStatus.PAYMENT_PENDING);

            case "delivery" -> List.of(
                    JobCardStatus.PAYMENT_COMPLETED,
                    JobCardStatus.READY_FOR_DELIVERY,
                    JobCardStatus.WORK_COMPLETED,
                    JobCardStatus.DELIVERED);

            // "Needs attention": every state where the job is waiting on
            // someone at the garage (or on the customer) to act, rather
            // than on work already in progress.
            case "attention" -> List.of(
                    JobCardStatus.OPEN,
                    JobCardStatus.INSPECTION_PENDING,
                    JobCardStatus.INSPECTION_COMPLETED,
                    JobCardStatus.ESTIMATE_PENDING,
                    JobCardStatus.WAITING_FOR_APPROVAL,
                    JobCardStatus.REPAIR_PENDING,
                    JobCardStatus.REPAIR_COMPLETED,
                    JobCardStatus.READY_FOR_INVOICE,
                    JobCardStatus.INVOICE_GENERATED,
                    JobCardStatus.READY_FOR_DELIVERY);

            case "active" -> Arrays.stream(JobCardStatus.values())
                    .filter(value -> value != JobCardStatus.CLOSED
                            && value != JobCardStatus.CANCELLED)
                    .toList();

            default -> List.of();
        };
    }

    private JobCardStatus parseStatusOrNull(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return JobCardStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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
    @PreAuthorize(JOBCARD_OPERATIONAL_ROLES)
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
    @PreAuthorize(JOBCARD_OPERATIONAL_ROLES)
    public ResponseEntity<JobCardResponse> qualityCheck(
            @PathVariable String jobCardNumber) {

        return ResponseEntity.ok(jobCardService.performQualityCheck(jobCardNumber));
    }

    @PostMapping("/{jobCardNumber}/quality-check/pass")
    @PreAuthorize(JOBCARD_OPERATIONAL_ROLES)
    @Operation(summary = "Pass Quality Check")
    public ResponseEntity<ApiResponse<QualityCheckResponse>> passQualityCheck(
            @PathVariable String jobCardNumber,
            @Valid @RequestBody CreateQualityCheckRequest request) {

        QualityCheckResponse response =
                qualityCheckService.passQualityCheck(jobCardNumber, request);

        return ApiResponseUtil.success(
                "Quality Check passed successfully.",
                response);
    }

    @PostMapping("/{jobCardNumber}/quality-check/fail")
    @PreAuthorize(JOBCARD_OPERATIONAL_ROLES)
    @Operation(summary = "Fail Quality Check")
    public ResponseEntity<ApiResponse<QualityCheckResponse>> failQualityCheck(
            @PathVariable String jobCardNumber,
            @Valid @RequestBody CreateQualityCheckRequest request) {

        QualityCheckResponse response =
                qualityCheckService.failQualityCheck(jobCardNumber, request);

        return ApiResponseUtil.success(
                "Quality Check failed.",
                response);
    }

    @GetMapping("/{jobCardNumber}/quality-check")
    @Operation(summary = "Get Quality Check")
    public ResponseEntity<ApiResponse<QualityCheckResponse>> getQualityCheck(
            @PathVariable String jobCardNumber) {

        QualityCheckResponse response =
                qualityCheckService.getQualityCheck(jobCardNumber);

        return ApiResponseUtil.success(
                "Quality Check fetched successfully.",
                response);
    }


}