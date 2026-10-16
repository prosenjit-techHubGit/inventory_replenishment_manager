package com.example.procurement.application;

import com.example.procurement.application.dto.ReplenishmentCheckResult;
import com.example.procurement.domain.inventory.InventoryItem;
import com.example.procurement.domain.purchaseorder.PurchaseOrderStatus;
import com.example.procurement.domain.recommendation.RecommendationStatus;
import com.example.procurement.exception.NotFoundException;
import com.example.procurement.repository.InventoryItemRepository;
import com.example.procurement.repository.PurchaseOrderRepository;
import com.example.procurement.repository.RecommendationRepository;
import com.example.procurement.repository.ReplenishmentClaimRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Candidate discovery and per-item claim-protected replenishment evaluation (plan sections 7, 39, 49). */
@Service
public class ReplenishmentApplicationService {

    private static final List<PurchaseOrderStatus> OPEN_SUPPLY_STATUSES = List.of(
            PurchaseOrderStatus.APPROVED,
            PurchaseOrderStatus.PENDING_SUPPLIER_CONFIRMATION,
            PurchaseOrderStatus.SUPPLIER_DATE_REVISED,
            PurchaseOrderStatus.SUPPLIER_CONFIRMED,
            PurchaseOrderStatus.RELEASED,
            PurchaseOrderStatus.IN_FULFILLMENT,
            PurchaseOrderStatus.PARTIALLY_RECEIVED);

    private static final List<RecommendationStatus> ACTIVE_RECOMMENDATION_STATUSES =
            List.of(RecommendationStatus.PENDING_REVIEW, RecommendationStatus.ACCEPTED);

    private static final Duration CLAIM_TTL = Duration.ofMinutes(10);

    private final InventoryItemRepository inventoryItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final RecommendationRepository recommendationRepository;
    private final ReplenishmentClaimRepository replenishmentClaimRepository;

    public ReplenishmentApplicationService(
            InventoryItemRepository inventoryItemRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            RecommendationRepository recommendationRepository,
            ReplenishmentClaimRepository replenishmentClaimRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.recommendationRepository = recommendationRepository;
        this.replenishmentClaimRepository = replenishmentClaimRepository;
    }

    /** Job worker `list-replenishment-candidates`: cheap pre-filter; the real decision happens per item below. */
    @Transactional(readOnly = true)
    public List<UUID> listCandidateItemIds() {
        return inventoryItemRepository.findCandidateItemIds();
    }

    /**
     * Job worker `check-replenishment` body (plan section 49.4, steps 1-5). On exit, either the
     * claim is held open (caller must create a recommendation and release it) or it has already
     * been released because no recommendation is needed this run.
     */
    @Transactional
    public ReplenishmentCheckResult checkReplenishment(UUID itemId, String runId) {
        OffsetDateTime now = OffsetDateTime.now();
        int acquired = replenishmentClaimRepository.tryAcquireClaim(itemId, runId, now, now.plus(CLAIM_TTL));
        if (acquired == 0) {
            // Another run already holds an active claim for this item; skip, next run will retry.
            return new ReplenishmentCheckResult(itemId, null, null, null, false);
        }

        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found: " + itemId));
        BigDecimal openPurchaseQuantity = purchaseOrderRepository.sumOpenPurchaseQuantity(itemId, OPEN_SUPPLY_STATUSES);
        BigDecimal projectedAvailable = item.getOnHandQuantity().add(openPurchaseQuantity);
        boolean required = projectedAvailable.compareTo(item.getReorderLevel()) < 0;
        boolean alreadyCovered = recommendationRepository.existsByItemIdAndStatusIn(itemId, ACTIVE_RECOMMENDATION_STATUSES);

        if (!required || alreadyCovered) {
            replenishmentClaimRepository.releaseClaim(itemId, runId);
            return new ReplenishmentCheckResult(
                    itemId, item.getOnHandQuantity(), item.getReorderLevel(), openPurchaseQuantity, false);
        }
        return new ReplenishmentCheckResult(
                itemId, item.getOnHandQuantity(), item.getReorderLevel(), openPurchaseQuantity, true);
    }

    @Transactional
    public void releaseClaim(UUID itemId, String runId) {
        replenishmentClaimRepository.releaseClaim(itemId, runId);
    }
}
