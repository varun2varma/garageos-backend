package com.garageos.modules.catalog.service;

import com.garageos.core.enums.catalog.CatalogItemType;
import com.garageos.core.exception.BusinessException;
import com.garageos.modules.catalog.dto.request.ClassifyCatalogItemRequest;
import com.garageos.modules.catalog.dto.request.CreateCatalogItemRequest;
import com.garageos.modules.catalog.dto.request.CreateTaxRuleRequest;
import com.garageos.modules.catalog.dto.request.SetActiveRequest;
import com.garageos.modules.catalog.dto.request.UpdateCatalogItemRequest;
import com.garageos.modules.catalog.dto.request.UpdateTaxRuleRequest;
import com.garageos.modules.catalog.dto.response.CatalogItemResponse;
import com.garageos.modules.catalog.dto.response.TaxRuleResponse;
import com.garageos.modules.catalog.entity.CatalogItem;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface CatalogService {

    /** Bounded, paginated lookup of ACTIVE items of one type; {@code query} matches the normalised name. */
    Page<CatalogItemResponse> search(String itemType, String query, int page, int size);

    /** Platform administrators: every item, filterable by type, query, category, status and active flag. */
    Page<CatalogItemResponse> adminSearch(
            String itemType, String query, String category, String status, Boolean active, int page, int size);

    /** One item; staff see only active items, platform administrators see all. */
    CatalogItemResponse get(Long id);

    /** Platform reviewers only. */
    CatalogItemResponse create(CreateCatalogItemRequest request);

    /** Platform reviewers only. */
    CatalogItemResponse update(Long id, UpdateCatalogItemRequest request);

    /** Deactivating stops new selection only; existing estimate and invoice lines are untouched. */
    CatalogItemResponse setActive(Long id, SetActiveRequest request);

    /** Propose or verify the HSN/SAC code and tax rule of one item. */
    CatalogItemResponse classify(Long id, ClassifyCatalogItemRequest request);

    List<TaxRuleResponse> listTaxRules();

    TaxRuleResponse createTaxRule(CreateTaxRuleRequest request);

    /** Description, source and active flag only; rate and dates are immutable. */
    TaxRuleResponse updateTaxRule(Long id, UpdateTaxRuleRequest request);

    /** The catalog item, which must exist, be active and be of {@code expected} type. */
    CatalogItem requireUsable(Long id, CatalogItemType expected);

    /** Rate of the rule in force on {@code on}; empty when the code is blank or no rule applies. */
    Optional<BigDecimal> resolveRate(String ruleCode, LocalDate on);

    CatalogItemResponse toResponse(CatalogItem item);

    /**
     * Validates and applies HSN/SAC code and tax rule to {@code item}. Verifying needs a code,
     * an existing rule and a source; it records who verified and when.
     */
    void applyClassification(
            CatalogItem item, String hsnSacCode, String taxRuleCode, boolean verified, String source);

    /**
     * The one rule for an HSN/SAC code held by a catalog item: 4 to 8 digits (same as the request DTOs and the
     * ck_catalog_item_code_format database check); a SERVICE needs a SAC code (99...), a PART an HSN code.
     */
    static void requireValidCode(CatalogItemType type, String code) {
        if (code == null || !code.matches("^[0-9]{4,8}$")) {
            throw new BusinessException("HSN/SAC must be 4 to 8 digits.");
        }
        boolean serviceCode = code.startsWith("99");
        if (type == CatalogItemType.SERVICE && !serviceCode) {
            throw new BusinessException("A service needs a SAC code (starting with 99).");
        }
        if (type == CatalogItemType.PART && serviceCode) {
            throw new BusinessException("A part needs an HSN code; 99xx codes are service (SAC) codes.");
        }
    }

    /** Lower-cased, trimmed, single-spaced form used for search and duplicate detection. */
    static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
