package com.garageos.modules.catalog.service.impl;

import com.garageos.core.enums.catalog.CatalogItemType;
import com.garageos.core.enums.catalog.ClassificationStatus;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.catalog.dto.request.ClassifyCatalogItemRequest;
import com.garageos.modules.catalog.dto.request.CreateCatalogItemRequest;
import com.garageos.modules.catalog.dto.request.CreateTaxRuleRequest;
import com.garageos.modules.catalog.dto.request.SetActiveRequest;
import com.garageos.modules.catalog.dto.request.UpdateCatalogItemRequest;
import com.garageos.modules.catalog.dto.request.UpdateTaxRuleRequest;
import com.garageos.modules.catalog.dto.response.CatalogItemResponse;
import com.garageos.modules.catalog.dto.response.TaxRuleResponse;
import com.garageos.modules.catalog.entity.CatalogItem;
import com.garageos.modules.catalog.entity.GstTaxRule;
import com.garageos.modules.catalog.repository.CatalogItemRepository;
import com.garageos.modules.catalog.repository.GstTaxRuleRepository;
import com.garageos.modules.catalog.service.CatalogService;
import com.garageos.modules.catalog.service.CatalogUnits;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private static final java.util.regex.Pattern HSN_SAC_FORMAT = java.util.regex.Pattern.compile("^[0-9]{4,8}$");

    static final int MAX_PAGE_SIZE = 50;

    private final CatalogItemRepository repository;
    private final GstTaxRuleRepository taxRuleRepository;
    private final GarageScope garageScope;

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogItemResponse> search(String itemType, String query, int page, int size) {

        CatalogItemType type = parseType(itemType);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        String q = searchText(query);

        Page<CatalogItem> result = q.isEmpty()
                ? repository.findByItemTypeAndActiveTrueOrderByNameAsc(type, pageable)
                : repository.search(type, "%" + q + "%", pageable);

        return result.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogItemResponse> adminSearch(
            String itemType, String query, String category, String status, Boolean active, int page, int size) {

        requireAdmin();

        CatalogItemType type = itemType == null || itemType.isBlank() ? null : parseType(itemType);
        ClassificationStatus parsedStatus = parseStatus(status);
        String q = searchText(query);
        String cat = category == null || category.isBlank() ? null : category.trim().toLowerCase(Locale.ROOT);

        Specification<CatalogItem> spec = (root, cq, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (type != null) {
                p.add(cb.equal(root.get("itemType"), type));
            }
            if (parsedStatus != null) {
                p.add(cb.equal(root.get("classificationStatus"), parsedStatus));
            }
            if (active != null) {
                p.add(cb.equal(root.get("active"), active));
            }
            if (cat != null) {
                p.add(cb.equal(cb.lower(root.get("category")), cat));
            }
            if (!q.isEmpty()) {
                p.add(cb.like(root.get("nameNormalized"), "%" + q + "%"));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("name").ascending());

        return repository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogItemResponse get(Long id) {

        CatalogItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found."));

        if (!item.isActive() && !isAdmin()) {
            throw new ResourceNotFoundException("Catalog item not found.");
        }

        return toResponse(item);
    }

    @Override
    @Transactional
    public CatalogItemResponse create(CreateCatalogItemRequest request) {

        requireAdmin();

        CatalogItemType type = parseType(request.getItemType());
        String normalized = CatalogService.normalize(request.getName());

        if (repository.findByItemTypeAndNameNormalized(type, normalized).isPresent()) {
            throw new BusinessException("A catalog " + type.name().toLowerCase(Locale.ROOT)
                    + " with this name already exists.");
        }

        CatalogItem item = new CatalogItem();
        item.setItemType(type);
        item.setName(request.getName().trim());
        item.setNameNormalized(normalized);
        item.setDescription(blankToNull(request.getDescription()));
        item.setDefaultUnit(CatalogUnits.normalize(request.getDefaultUnit()));
        item.setCategory(blankToNull(request.getCategory()));
        item.setCreatedByUserId(garageScope.principal().getId());

        applyClassification(item, request.getHsnSacCode(), request.getTaxRuleCode(),
                Boolean.TRUE.equals(request.getVerified()), request.getClassificationSource());

        return toResponse(repository.save(item));
    }

    @Override
    @Transactional
    public CatalogItemResponse update(Long id, UpdateCatalogItemRequest request) {

        requireAdmin();

        CatalogItem item = find(id);
        checkVersion(item, request.getVersion());

        String normalized = CatalogService.normalize(request.getName());

        repository.findByItemTypeAndNameNormalized(item.getItemType(), normalized)
                .filter(other -> !other.getId().equals(item.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Another catalog item already uses this name.");
                });

        item.setName(request.getName().trim());
        item.setNameNormalized(normalized);
        item.setDescription(blankToNull(request.getDescription()));
        item.setDefaultUnit(CatalogUnits.normalize(request.getDefaultUnit()));
        item.setCategory(blankToNull(request.getCategory()));
        item.setActive(Boolean.TRUE.equals(request.getActive()));
        item.setUpdatedByUserId(garageScope.principal().getId());

        applyClassification(item, request.getHsnSacCode(), request.getTaxRuleCode(),
                Boolean.TRUE.equals(request.getVerified()), request.getClassificationSource());

        return toResponse(repository.save(item));
    }

    @Override
    @Transactional
    public CatalogItemResponse setActive(Long id, SetActiveRequest request) {

        requireAdmin();

        CatalogItem item = find(id);
        checkVersion(item, request.getVersion());

        item.setActive(Boolean.TRUE.equals(request.getActive()));
        item.setUpdatedByUserId(garageScope.principal().getId());

        return toResponse(repository.save(item));
    }

    @Override
    @Transactional
    public CatalogItemResponse classify(Long id, ClassifyCatalogItemRequest request) {

        requireAdmin();

        CatalogItem item = find(id);
        checkVersion(item, request.getVersion());

        applyClassification(item, request.getHsnSacCode(), request.getTaxRuleCode(),
                Boolean.TRUE.equals(request.getVerified()), request.getClassificationSource());

        item.setUpdatedByUserId(garageScope.principal().getId());

        return toResponse(repository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxRuleResponse> listTaxRules() {

        requireAdmin();

        return taxRuleRepository.findAllByOrderByRuleCodeAscEffectiveFromDesc().stream()
                .map(CatalogServiceImpl::toRuleResponse)
                .toList();
    }

    @Override
    @Transactional
    public TaxRuleResponse createTaxRule(CreateTaxRuleRequest request) {

        requireAdmin();

        String code = request.getRuleCode().trim().toUpperCase(Locale.ROOT);

        List<GstTaxRule> existing = taxRuleRepository.findByRuleCodeOrderByEffectiveFromDesc(code);

        if (existing.stream().anyMatch(r -> r.getEffectiveFrom().equals(request.getEffectiveFrom()))) {
            throw new BusinessException("A rule with this code already starts on that date.");
        }

        boolean laterRuleExists = existing.stream()
                .anyMatch(r -> r.getEffectiveFrom().isAfter(request.getEffectiveFrom()));

        if (laterRuleExists) {
            throw new BusinessException("A later rule already exists for this code; add rates in date order.");
        }

        // A new rate supersedes the open-ended one in force before it; older rows are only end-dated.
        existing.stream()
                .filter(r -> r.getEffectiveTo() == null)
                .forEach(r -> {
                    r.setEffectiveTo(request.getEffectiveFrom().minusDays(1));
                    taxRuleRepository.save(r);
                });

        GstTaxRule rule = new GstTaxRule();
        rule.setRuleCode(code);
        rule.setDescription(blankToNull(request.getDescription()));
        rule.setRatePercent(request.getRatePercent());
        rule.setEffectiveFrom(request.getEffectiveFrom());
        rule.setSourceReference(blankToNull(request.getSourceReference()));
        rule.setVerifiedOn(request.getVerifiedOn());
        rule.setCreatedByUserId(garageScope.principal().getId());

        return toRuleResponse(taxRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public TaxRuleResponse updateTaxRule(Long id, UpdateTaxRuleRequest request) {

        requireAdmin();

        GstTaxRule rule = taxRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax rule not found."));

        boolean deactivating = rule.isActive() && !Boolean.TRUE.equals(request.getActive());

        if (deactivating && repository.countByTaxRuleCodeAndClassificationStatus(
                rule.getRuleCode(), ClassificationStatus.VERIFIED) > 0) {
            throw new BusinessException(
                    "Verified catalog items still use this rule. Re-classify them before deactivating it.");
        }

        rule.setDescription(blankToNull(request.getDescription()));
        rule.setSourceReference(blankToNull(request.getSourceReference()));
        rule.setVerifiedOn(request.getVerifiedOn());
        rule.setActive(Boolean.TRUE.equals(request.getActive()));

        return toRuleResponse(taxRuleRepository.save(rule));
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogItem requireUsable(Long id, CatalogItemType expected) {

        CatalogItem item = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found."));

        if (!item.isActive()) {
            throw new BusinessException("This catalog item is no longer active.");
        }

        if (item.getItemType() != expected) {
            throw new BusinessException("This catalog item is a "
                    + item.getItemType().name().toLowerCase(Locale.ROOT)
                    + " and cannot be used for this kind of line.");
        }

        return item;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> resolveRate(String ruleCode, LocalDate on) {

        if (ruleCode == null || ruleCode.isBlank()) {
            return Optional.empty();
        }

        return taxRuleRepository.findEffective(ruleCode, on, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(GstTaxRule::getRatePercent);
    }

    @Override
    public CatalogItemResponse toResponse(CatalogItem item) {
        return CatalogItemResponse.builder()
                .id(item.getId())
                .itemType(item.getItemType().name())
                .name(item.getName())
                .description(item.getDescription())
                .defaultUnit(item.getDefaultUnit())
                .category(item.getCategory())
                .hsnSacCode(item.getHsnSacCode())
                .classificationStatus(item.getClassificationStatus().name())
                .classificationSource(item.getClassificationSource())
                .classificationVerifiedAt(item.getClassificationVerifiedAt())
                .taxRuleCode(item.getTaxRuleCode())
                .currentRatePercent(resolveRate(item.getTaxRuleCode(), LocalDate.now()).orElse(null))
                .active(item.isActive())
                .version(item.getVersion())
                .build();
    }

    /**
     * Classification and tax rule are validated together: a PART takes an HSN
     * (never a 99xxxx service code), a SERVICE takes a SAC (99xxxx), and a
     * VERIFIED item must have a code, an existing tax rule and a source. Reviewers
     * supply the values; nothing is inferred here. Verifying records who and when;
     * un-verifying clears that record.
     */
    @Override
    public void applyClassification(
            CatalogItem item, String hsnSacCode, String taxRuleCode, boolean verified, String source) {

        String code = blankToNull(hsnSacCode);
        String rule = taxRuleCode == null || taxRuleCode.isBlank()
                ? null
                : taxRuleCode.trim().toUpperCase(Locale.ROOT);
        String sourceText = blankToNull(source);

        if (code != null) {
            // Same rule as the request DTOs and the ck_catalog_item_code_format database check, applied here so
            // that the CSV import (which bypasses the DTO validation) rejects a malformed code in dry run and
            // real run alike instead of failing at save time.
            if (!HSN_SAC_FORMAT.matcher(code).matches()) {
                throw new BusinessException("HSN/SAC must be 4 to 8 digits.");
            }

            boolean serviceCode = code.startsWith("99");
            if (item.getItemType() == CatalogItemType.SERVICE && !serviceCode) {
                throw new BusinessException("A service needs a SAC code (starting with 99).");
            }
            if (item.getItemType() == CatalogItemType.PART && serviceCode) {
                throw new BusinessException("A part needs an HSN code; 99xx codes are service (SAC) codes.");
            }
        }

        if (rule != null && !taxRuleRepository.existsByRuleCode(rule)) {
            throw new BusinessException("Unknown tax rule: " + rule);
        }

        if (verified && (code == null || rule == null)) {
            throw new BusinessException("Verification needs both an HSN/SAC code and a tax rule.");
        }

        if (verified && sourceText == null) {
            throw new BusinessException("Verification needs the source the classification was confirmed from.");
        }

        // Re-saving an unchanged verified classification (for example while editing the name)
        // keeps who verified it and when.
        boolean unchangedVerified = verified
                && item.getClassificationStatus() == ClassificationStatus.VERIFIED
                && java.util.Objects.equals(item.getHsnSacCode(), code)
                && java.util.Objects.equals(item.getTaxRuleCode(), rule)
                && java.util.Objects.equals(item.getClassificationSource(), sourceText)
                && item.getClassificationVerifiedAt() != null;

        item.setHsnSacCode(code);
        item.setTaxRuleCode(rule);
        item.setClassificationSource(sourceText);

        if (unchangedVerified) {
            return;
        }

        if (verified) {
            item.setClassificationStatus(ClassificationStatus.VERIFIED);
            item.setClassificationVerifiedAt(LocalDateTime.now());
            item.setClassificationVerifiedByUserId(garageScope.principal().getId());
        } else {
            item.setClassificationStatus(code != null
                    ? ClassificationStatus.PENDING_REVIEW
                    : ClassificationStatus.UNCLASSIFIED);
            item.setClassificationVerifiedAt(null);
            item.setClassificationVerifiedByUserId(null);
        }
    }

    private CatalogItem find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found."));
    }

    private static void checkVersion(CatalogItem item, Long version) {
        if (version != null && !version.equals(item.getVersion())) {
            throw new BusinessException(
                    "This item was changed by someone else. Reload it and try again.");
        }
    }

    private boolean isAdmin() {
        return garageScope.isUnrestricted(garageScope.principal());
    }

    /** Defence in depth behind the controller's SUPER_ADMIN rule. */
    private void requireAdmin() {
        if (!isAdmin()) {
            throw new ResourceNotFoundException("Not found.");
        }
    }

    private static String searchText(String query) {
        return CatalogService.normalize(query).replace("%", "").replace("_", "").replace("\\", "");
    }

    private static CatalogItemType parseType(String raw) {
        try {
            return CatalogItemType.valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Item type must be PART or SERVICE.");
        }
    }

    private static ClassificationStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ClassificationStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Status must be UNCLASSIFIED, PENDING_REVIEW or VERIFIED.");
        }
    }

    private static TaxRuleResponse toRuleResponse(GstTaxRule rule) {
        return TaxRuleResponse.builder()
                .id(rule.getId())
                .ruleCode(rule.getRuleCode())
                .description(rule.getDescription())
                .ratePercent(rule.getRatePercent())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .sourceReference(rule.getSourceReference())
                .verifiedOn(rule.getVerifiedOn())
                .active(rule.isActive())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
