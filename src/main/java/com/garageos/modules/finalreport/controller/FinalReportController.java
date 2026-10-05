package com.garageos.modules.finalreport.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.enums.report.FinalReportAudience;
import com.garageos.modules.finalreport.dto.response.FinalReportStatusResponse;
import com.garageos.modules.finalreport.service.FinalReportService;
import com.garageos.modules.finalreport.service.FinalReportService.FinalReportFile;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Final Job Card reports. There is no audience parameter to trust: each
 * endpoint is bound to one audience, and the service decides from the caller's
 * relationship to the Job Card (garage staff role / owning customer) whether
 * they may read it. Customers can only ever read the customer report.
 */
@RestController
@RequestMapping("/api/v1/job-cards/{jobCardId}/final-report")
@RequiredArgsConstructor
public class FinalReportController {

    private final FinalReportService finalReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<FinalReportStatusResponse>> status(
            @AuthenticationPrincipal GarageUserPrincipal principal,
            @PathVariable Long jobCardId) {

        return ApiResponseUtil.success(
                "Final report status fetched successfully.",
                finalReportService.getStatus(principal, jobCardId));
    }

    @GetMapping("/customer")
    public ResponseEntity<byte[]> customerReport(
            @AuthenticationPrincipal GarageUserPrincipal principal,
            @PathVariable Long jobCardId,
            @RequestParam(required = false) Integer version) {

        return pdf(finalReportService.download(principal, jobCardId, FinalReportAudience.CUSTOMER, version));
    }

    @GetMapping("/owner")
    public ResponseEntity<byte[]> ownerReport(
            @AuthenticationPrincipal GarageUserPrincipal principal,
            @PathVariable Long jobCardId,
            @RequestParam(required = false) Integer version) {

        return pdf(finalReportService.download(principal, jobCardId, FinalReportAudience.OWNER, version));
    }

    @PostMapping("/regenerate")
    public ResponseEntity<ApiResponse<FinalReportStatusResponse>> regenerate(
            @AuthenticationPrincipal GarageUserPrincipal principal,
            @PathVariable Long jobCardId) {

        return ApiResponseUtil.success(
                "Final report generated successfully.",
                finalReportService.regenerate(principal, jobCardId));
    }

    private static ResponseEntity<byte[]> pdf(FinalReportFile file) {

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.fileName()).build().toString())
                .header("X-Report-Version", String.valueOf(file.version()))
                .cacheControl(CacheControl.noStore())
                .body(file.content());
    }
}
