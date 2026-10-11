package com.garageos.modules.catalog.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.modules.catalog.dto.request.ClassifyCatalogItemRequest;
import com.garageos.modules.catalog.dto.request.CreateCatalogItemRequest;
import com.garageos.modules.catalog.dto.request.ImportCatalogRequest;
import com.garageos.modules.catalog.dto.request.SetActiveRequest;
import com.garageos.modules.catalog.dto.request.UpdateTaxRuleRequest;
import com.garageos.modules.catalog.dto.request.CreateTaxRuleRequest;
import com.garageos.modules.catalog.dto.request.LinkCatalogItemRequest;
import com.garageos.modules.catalog.dto.request.PromoteManualItemRequest;
import com.garageos.modules.catalog.dto.request.UpdateCatalogItemRequest;
import com.garageos.modules.catalog.dto.response.CatalogItemResponse;
import com.garageos.modules.catalog.dto.response.ImportReportResponse;
import com.garageos.modules.catalog.dto.response.ReviewItemResponse;
import com.garageos.modules.catalog.dto.response.TaxRuleResponse;
import com.garageos.modules.catalog.service.CatalogImportService;
import com.garageos.modules.catalog.service.EstimateLineClassificationService;
import com.garageos.modules.catalog.dto.request.CorrectEstimateLineClassificationRequest;
import com.garageos.modules.catalog.service.CatalogReviewService;
import com.garageos.modules.catalog.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Central parts and services catalog. Staff look items up (bounded, paginated);
 * only platform administrators (SUPER_ADMIN) create or change global catalog
 * data, tax rules and review manual entries - a garage user can never mutate
 * the catalog other garages rely on.
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private static final String ADMIN = "hasRole('SUPER_ADMIN')";

    private static final String LOOKUP_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'OWNER', 'MANAGER', 'SERVICE_ADVISOR', 'TECHNICIAN', 'INVENTORY_MANAGER')";

    private final CatalogService catalogService;
    private final CatalogReviewService reviewService;
    private final CatalogImportService importService;
    private final EstimateLineClassificationService classificationCorrectionService;

    @GetMapping("/items")
    @PreAuthorize(LOOKUP_ROLES)
    public ResponseEntity<ApiResponse<Page<CatalogItemResponse>>> search(
            @RequestParam String type,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponseUtil.success(
                "Catalog items fetched successfully.",
                catalogService.search(type, q, page, size));
    }

    @PostMapping("/items")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<CatalogItemResponse>> create(
            @Valid @RequestBody CreateCatalogItemRequest request) {

        return ApiResponseUtil.created("Catalog item created successfully.", catalogService.create(request));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<CatalogItemResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCatalogItemRequest request) {

        return ApiResponseUtil.success("Catalog item updated successfully.", catalogService.update(id, request));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize(LOOKUP_ROLES)
    public ResponseEntity<ApiResponse<CatalogItemResponse>> get(@PathVariable Long id) {
        return ApiResponseUtil.success("Catalog item fetched successfully.", catalogService.get(id));
    }

    /** Administrators browse the whole catalog, including inactive and unclassified items. */
    @GetMapping("/admin/items")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<Page<CatalogItemResponse>>> adminSearch(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponseUtil.success(
                "Catalog items fetched successfully.",
                catalogService.adminSearch(type, q, category, status, active, page, size));
    }

    @PatchMapping("/items/{id}/active")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<CatalogItemResponse>> setActive(
            @PathVariable Long id,
            @Valid @RequestBody SetActiveRequest request) {

        return ApiResponseUtil.success("Catalog item updated successfully.", catalogService.setActive(id, request));
    }

    @PostMapping("/items/{id}/classification")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<CatalogItemResponse>> classify(
            @PathVariable Long id,
            @Valid @RequestBody ClassifyCatalogItemRequest request) {

        return ApiResponseUtil.success("Classification saved successfully.", catalogService.classify(id, request));
    }

    @PatchMapping("/tax-rules/{id}")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<TaxRuleResponse>> updateTaxRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaxRuleRequest request) {

        return ApiResponseUtil.success("Tax rule updated successfully.", catalogService.updateTaxRule(id, request));
    }

    /** Validates (default) or saves a CSV of new catalog names; never overwrites an existing item. */
    @PostMapping("/import")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<ImportReportResponse>> importCsv(
            @Valid @RequestBody ImportCatalogRequest request) {

        return ApiResponseUtil.success("Import checked successfully.", importService.importCsv(request));
    }

    @GetMapping("/tax-rules")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<List<TaxRuleResponse>>> taxRules() {
        return ApiResponseUtil.success("Tax rules fetched successfully.", catalogService.listTaxRules());
    }

    @PostMapping("/tax-rules")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<TaxRuleResponse>> createTaxRule(
            @Valid @RequestBody CreateTaxRuleRequest request) {

        return ApiResponseUtil.created("Tax rule created successfully.", catalogService.createTaxRule(request));
    }

    @GetMapping("/review/estimate-items")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<Page<ReviewItemResponse>>> pendingReview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponseUtil.success("Items awaiting review fetched successfully.",
                reviewService.pending(page, size));
    }

    @PostMapping("/review/estimate-items/{id}/link")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<ReviewItemResponse>> link(
            @PathVariable Long id,
            @Valid @RequestBody LinkCatalogItemRequest request) {

        return ApiResponseUtil.success("Item linked to the catalog.",
                reviewService.link(id, request.getCatalogItemId()));
    }

    @PostMapping("/review/estimate-items/{id}/promote")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<ReviewItemResponse>> promote(
            @PathVariable Long id,
            @Valid @RequestBody PromoteManualItemRequest request) {

        return ApiResponseUtil.success("Item added to the catalog and linked.",
                reviewService.promote(id, request));
    }

    /**
     * Audited classification correction for one line of an APPROVED estimate that has no invoice. SUPER_ADMIN only.
     * 409 when the estimate is invoiced, not approved, or the correction would change the approved tax.
     */
    @PostMapping("/review/estimates/{estimateId}/items/{estimateItemId}/classification-correction")
    @PreAuthorize(ADMIN)
    public ResponseEntity<ApiResponse<ReviewItemResponse>> correctApprovedLine(
            @PathVariable Long estimateId,
            @PathVariable Long estimateItemId,
            @Valid @RequestBody CorrectEstimateLineClassificationRequest request) {

        return ApiResponseUtil.success("Estimate line classification corrected.",
                classificationCorrectionService.correct(estimateId, estimateItemId, request));
    }
}
