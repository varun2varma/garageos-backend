package com.garageos.modules.catalog.controller;

import com.garageos.core.api.response.ApiResponse;
import com.garageos.core.api.response.ApiResponseUtil;
import com.garageos.core.enums.catalog.ClassificationStatus;
import com.garageos.core.tax.TaxPolicy;
import com.garageos.modules.catalog.repository.CatalogItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Read-only visibility of how unverified GST classification is treated RIGHT NOW, so an operator or Owner can
 * see it instead of discovering it in an invoice. It changes nothing and holds no tax data of its own.
 */
@RestController
@RequestMapping("/api/v1/catalog/tax-policy")
@RequiredArgsConstructor
public class TaxPolicyStatusController {

    private final TaxPolicy policy;
    private final CatalogItemRepository catalogItemRepository;

    public record Status(
            boolean requireClassification,
            BigDecimal interimFallbackRatePercent,
            boolean interimFallbackActive,
            long unclassifiedCatalogItems,
            long pendingReviewCatalogItems,
            long verifiedCatalogItems,
            String note) {
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<Status>> status() {

        boolean fallbackActive = !policy.requireClassification() && policy.fallbackRatePercent().isPresent();

        String note = policy.requireClassification()
                ? "Strict: an invoice cannot be issued while a taxable line lacks a verified classification and rate."
                : fallbackActive
                ? "INTERIM: taxable lines without a verified classification are billed at the configured fallback rate. "
                  + "That rate is NOT verified. Consumables (OTHERS) carry no GST by a legacy placeholder rule."
                : "No fallback: taxable lines without a verified rate cannot be added.";

        return ApiResponseUtil.success("Tax policy status fetched.", new Status(
                policy.requireClassification(),
                policy.fallbackRatePercent().orElse(null),
                fallbackActive,
                catalogItemRepository.countByClassificationStatus(ClassificationStatus.UNCLASSIFIED),
                catalogItemRepository.countByClassificationStatus(ClassificationStatus.PENDING_REVIEW),
                catalogItemRepository.countByClassificationStatus(ClassificationStatus.VERIFIED),
                note));
    }
}
