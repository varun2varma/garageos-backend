package com.garageos.modules.inventory.service.impl;

import com.garageos.core.enums.inventory.StockMovementType;
import com.garageos.core.exception.BusinessException;
import com.garageos.core.exception.ResourceNotFoundException;
import com.garageos.core.security.GarageScope;
import com.garageos.modules.identity.entity.User;
import com.garageos.modules.identity.repository.UserRepository;
import com.garageos.modules.identity.security.principal.GarageUserPrincipal;
import com.garageos.modules.inventory.dto.request.CreateInventoryItemRequest;
import com.garageos.modules.inventory.dto.request.StockMovementRequest;
import com.garageos.modules.inventory.dto.request.UpdateInventoryItemRequest;
import com.garageos.modules.inventory.dto.response.InventoryItemResponse;
import com.garageos.modules.inventory.dto.response.InventorySummaryResponse;
import com.garageos.modules.inventory.dto.response.StockMovementResponse;
import com.garageos.modules.inventory.entity.InventoryItem;
import com.garageos.modules.inventory.entity.StockMovement;
import com.garageos.modules.inventory.repository.InventoryItemRepository;
import com.garageos.modules.inventory.repository.StockMovementRepository;
import com.garageos.modules.inventory.service.InventoryService;
import com.garageos.modules.jobcard.entity.JobCard;
import com.garageos.modules.jobcard.repository.JobCardRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Inventory MVP. Every operation is scoped to one garage resolved through
 * {@link GarageScope} (the caller's own garage, or - for reads by an OWNER -
 * a garage they hold an active membership in). Roles are enforced at the
 * controller; garage isolation is enforced here.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int QTY_SCALE = 3;

    private final InventoryItemRepository itemRepository;
    private final StockMovementRepository movementRepository;
    private final JobCardRepository jobCardRepository;
    private final UserRepository userRepository;
    private final GarageScope garageScope;

    // ------------------------------------------------------------------
    // Catalogue
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public InventoryItemResponse createItem(CreateInventoryItemRequest request) {

        GarageUserPrincipal principal = garageScope.principal();
        Long garageId = garageScope.resolveGarageId(principal, null);

        String partNumber = normalizePartNumber(request.getPartNumber());

        if (partNumber != null && itemRepository.existsByGarageIdAndPartNumber(garageId, partNumber)) {
            throw new BusinessException("A part with this part number already exists.");
        }

        InventoryItem item = new InventoryItem();
        item.setGarageId(garageId);
        item.setName(request.getName().trim());
        item.setPartNumber(partNumber);
        item.setUnit(request.getUnit().trim());
        item.setReorderLevel(scale(request.getReorderLevel() == null
                ? BigDecimal.ZERO : request.getReorderLevel()));
        item.setQuantity(BigDecimal.ZERO.setScale(QTY_SCALE));
        item.setActive(true);

        item = itemRepository.saveAndFlush(item);

        BigDecimal opening = request.getOpeningQuantity();

        if (opening != null && opening.signum() > 0) {
            applyMovement(item, StockMovementType.STOCK_IN, scale(opening),
                    "Opening stock", null, principal);
        }

        return toResponse(item);
    }

    @Override
    @Transactional
    public InventoryItemResponse updateItem(Long id, UpdateInventoryItemRequest request) {

        GarageUserPrincipal principal = garageScope.principal();
        Long garageId = garageScope.resolveGarageId(principal, null);

        InventoryItem item = itemRepository.findByIdAndGarageId(id, garageId)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found."));

        String partNumber = normalizePartNumber(request.getPartNumber());

        if (partNumber != null
                && !partNumber.equals(item.getPartNumber())
                && itemRepository.existsByGarageIdAndPartNumber(garageId, partNumber)) {
            throw new BusinessException("A part with this part number already exists.");
        }

        item.setName(request.getName().trim());
        item.setPartNumber(partNumber);
        item.setUnit(request.getUnit().trim());
        item.setReorderLevel(scale(request.getReorderLevel()));
        item.setActive(request.getActive());

        return toResponse(itemRepository.saveAndFlush(item));
    }

    @Override
    public InventoryItemResponse getItem(Long id, Long garageId) {

        Long scoped = garageScope.resolveGarageId(garageScope.principal(), garageId);

        return toResponse(itemRepository.findByIdAndGarageId(id, scoped)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found.")));
    }

    @Override
    public Page<InventoryItemResponse> listItems(
            Long garageId, String search, String filter, boolean includeInactive,
            int page, int size) {

        Long scoped = garageScope.resolveGarageId(garageScope.principal(), garageId);

        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String mode = filter == null ? "ALL" : filter.trim().toUpperCase(Locale.ROOT);

        Specification<InventoryItem> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("garageId"), scoped));

            if (!includeInactive) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            if (!needle.isEmpty()) {
                String like = "%" + needle.replace("%", "").replace("_", "") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("partNumber"), "")), like)));
            }

            if ("OUT".equals(mode)) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("quantity"), BigDecimal.ZERO));
            } else if ("LOW".equals(mode)) {
                predicates.add(cb.greaterThan(root.<BigDecimal>get("quantity"), BigDecimal.ZERO));
                predicates.add(cb.lessThanOrEqualTo(
                        root.<BigDecimal>get("quantity"), root.<BigDecimal>get("reorderLevel")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(
                Math.max(page, 0), clampSize(size), Sort.by("name").ascending().and(Sort.by("id")));

        return itemRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    public InventorySummaryResponse getSummary(Long garageId) {

        Long scoped = garageScope.resolveGarageId(garageScope.principal(), garageId);

        return InventorySummaryResponse.builder()
                .totalActiveItems(itemRepository.countByGarageIdAndActiveTrue(scoped))
                .lowStockCount(itemRepository.countLowStock(scoped))
                .outOfStockCount(itemRepository.countOutOfStock(scoped))
                .build();
    }

    // ------------------------------------------------------------------
    // Movements
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public InventoryItemResponse recordMovement(Long itemId, StockMovementRequest request) {

        GarageUserPrincipal principal = garageScope.principal();
        Long garageId = garageScope.resolveGarageId(principal, null);

        // Row lock: concurrent movements on the same part serialise, so the
        // balance check below is race-free and stock can never go negative.
        InventoryItem item = itemRepository.findByIdAndGarageIdForUpdate(itemId, garageId)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found."));

        if (!item.isActive()) {
            throw new BusinessException("This part is inactive. Reactivate it before moving stock.");
        }

        BigDecimal quantity = scale(request.getQuantity());
        StockMovementType type = request.getType();
        String reason = request.getReason() == null || request.getReason().isBlank()
                ? null : request.getReason().trim();

        switch (type) {
            case STOCK_IN, STOCK_OUT -> {
                if (quantity.signum() <= 0) {
                    throw new BusinessException("Quantity must be greater than zero.");
                }
            }
            case ADJUSTMENT -> {
                if (quantity.signum() == 0) {
                    throw new BusinessException("An adjustment cannot be zero.");
                }
                if (reason == null) {
                    throw new BusinessException("A reason is required for a stock adjustment.");
                }
            }
        }

        JobCard jobCard = null;

        if (request.getJobCardNumber() != null && !request.getJobCardNumber().isBlank()) {

            if (type != StockMovementType.STOCK_OUT) {
                throw new BusinessException("Only issued stock can be linked to a job card.");
            }

            jobCard = jobCardRepository.findByJobCardNumber(request.getJobCardNumber().trim())
                    .filter(jc -> jc.getGarage() != null && garageId.equals(jc.getGarage().getId()))
                    .orElseThrow(() -> new BusinessException("Job card not found for this garage."));
        }

        applyMovement(item, type, quantity, reason, jobCard, principal);

        return toResponse(item);
    }

    @Override
    public Page<StockMovementResponse> listMovements(
            Long garageId, Long itemId, StockMovementType type, String jobCardNumber,
            int page, int size) {

        Long scoped = garageScope.resolveGarageId(garageScope.principal(), garageId);

        Long jobCardId = null;

        if (jobCardNumber != null && !jobCardNumber.isBlank()) {
            jobCardId = jobCardRepository.findByJobCardNumber(jobCardNumber.trim())
                    .filter(jc -> jc.getGarage() != null && scoped.equals(jc.getGarage().getId()))
                    .map(JobCard::getId)
                    // Unknown / foreign job card: match nothing.
                    .orElse(-1L);
        }

        final Long jobCardFilter = jobCardId;

        Specification<StockMovement> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("garageId"), scoped));

            if (itemId != null) {
                predicates.add(cb.equal(root.get("item").get("id"), itemId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (jobCardFilter != null) {
                predicates.add(cb.equal(root.get("jobCardId"), jobCardFilter));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(
                Math.max(page, 0), clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<StockMovement> movements = movementRepository.findAll(spec, pageable);

        Map<Long, String> userNames = userNames(movements.getContent());
        Map<Long, String> jobCardNumbers = jobCardNumbers(movements.getContent());

        return movements.map(m -> toMovementResponse(m, userNames, jobCardNumbers));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Applies the delta to the (already locked) item and writes the ledger row. */
    private void applyMovement(
            InventoryItem item, StockMovementType type, BigDecimal quantity,
            String reason, JobCard jobCard, GarageUserPrincipal principal) {

        BigDecimal delta = type == StockMovementType.STOCK_OUT ? quantity.negate() : quantity;
        BigDecimal balance = item.getQuantity().add(delta);

        if (balance.signum() < 0) {
            throw new BusinessException(
                    "Insufficient stock. Available: " + item.getQuantity().stripTrailingZeros().toPlainString()
                            + " " + item.getUnit() + ".");
        }

        item.setQuantity(balance);
        itemRepository.saveAndFlush(item);

        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setGarageId(item.getGarageId());
        movement.setType(type);
        movement.setQuantity(delta);
        movement.setBalanceAfter(balance);
        movement.setReason(reason);
        movement.setJobCardId(jobCard == null ? null : jobCard.getId());
        movement.setPerformedByUserId(principal.getId());

        movementRepository.save(movement);
    }

    private Map<Long, String> userNames(Collection<StockMovement> movements) {

        Set<Long> ids = movements.stream()
                .map(StockMovement::getPerformedByUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u ->
                        u.getLastName() == null || u.getLastName().isBlank()
                                ? u.getFirstName() : u.getFirstName() + " " + u.getLastName()));
    }

    private Map<Long, String> jobCardNumbers(Collection<StockMovement> movements) {

        Set<Long> ids = movements.stream()
                .map(StockMovement::getJobCardId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return Map.of();
        }

        return jobCardRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(JobCard::getId, JobCard::getJobCardNumber));
    }

    private InventoryItemResponse toResponse(InventoryItem i) {

        return InventoryItemResponse.builder()
                .id(i.getId())
                .garageId(i.getGarageId())
                .name(i.getName())
                .partNumber(i.getPartNumber())
                .unit(i.getUnit())
                .quantity(i.getQuantity())
                .reorderLevel(i.getReorderLevel())
                .active(i.isActive())
                .lowStock(i.isActive() && i.isLowStock())
                .outOfStock(i.isActive() && i.isOutOfStock())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }

    private StockMovementResponse toMovementResponse(
            StockMovement m, Map<Long, String> userNames, Map<Long, String> jobCardNumbers) {

        return StockMovementResponse.builder()
                .id(m.getId())
                .itemId(m.getItem().getId())
                .itemName(m.getItem().getName())
                .partNumber(m.getItem().getPartNumber())
                .unit(m.getItem().getUnit())
                .type(m.getType().name())
                .quantity(m.getQuantity())
                .balanceAfter(m.getBalanceAfter())
                .reason(m.getReason())
                .jobCardNumber(m.getJobCardId() == null ? null : jobCardNumbers.get(m.getJobCardId()))
                .performedByUserId(m.getPerformedByUserId())
                .performedByName(m.getPerformedByUserId() == null
                        ? null : userNames.get(m.getPerformedByUserId()))
                .createdAt(m.getCreatedAt())
                .build();
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(QTY_SCALE, java.math.RoundingMode.HALF_UP);
    }

    private static String normalizePartNumber(String partNumber) {
        return partNumber == null || partNumber.isBlank()
                ? null : partNumber.trim().toUpperCase(Locale.ROOT);
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
