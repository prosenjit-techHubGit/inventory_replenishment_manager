package com.example.procurement.application;

import com.example.procurement.ai.RecommendationProvider;
import com.example.procurement.ai.RecommendationRequest;
import com.example.procurement.ai.RecommendationRequest.EligibleSupplier;
import com.example.procurement.ai.RecommendationResponse;
import com.example.procurement.application.dto.RecommendationDto;
import com.example.procurement.camunda.mapper.RecommendationToPurchaseOrderMapper;
import com.example.procurement.domain.RiskLevel;
import com.example.procurement.domain.inventory.InventoryItem;
import com.example.procurement.domain.purchaseorder.PurchaseOrder;
import com.example.procurement.domain.recommendation.Recommendation;
import com.example.procurement.domain.recommendation.RecommendationStatus;
import com.example.procurement.domain.supplier.Supplier;
import com.example.procurement.exception.BusinessValidationException;
import com.example.procurement.exception.NotFoundException;
import com.example.procurement.repository.InventoryItemRepository;
import com.example.procurement.repository.PurchaseOrderRepository;
import com.example.procurement.repository.RecommendationRepository;
import com.example.procurement.repository.SupplierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns AI-backed recommendation creation, human review outcomes, and the accepted-recommendation
 * handoff to Process 2 (plan sections 7, 17, 44-46).
 */
@Service
public class RecommendationApplicationService {

    private final RecommendationRepository recommendationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final RecommendationProvider recommendationProvider;
    private final RecommendationToPurchaseOrderMapper recommendationToPurchaseOrderMapper;
    private final ProcessStartOutboxService processStartOutboxService;
    private final ReplenishmentApplicationService replenishmentApplicationService;
    private final PurchaseOrderApplicationService purchaseOrderApplicationService;

    public RecommendationApplicationService(
            RecommendationRepository recommendationRepository,
            InventoryItemRepository inventoryItemRepository,
            SupplierRepository supplierRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            RecommendationProvider recommendationProvider,
            RecommendationToPurchaseOrderMapper recommendationToPurchaseOrderMapper,
            ProcessStartOutboxService processStartOutboxService,
            ReplenishmentApplicationService replenishmentApplicationService,
            PurchaseOrderApplicationService purchaseOrderApplicationService) {
        this.recommendationRepository = recommendationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.recommendationProvider = recommendationProvider;
        this.recommendationToPurchaseOrderMapper = recommendationToPurchaseOrderMapper;
        this.processStartOutboxService = processStartOutboxService;
        this.replenishmentApplicationService = replenishmentApplicationService;
        this.purchaseOrderApplicationService = purchaseOrderApplicationService;
    }

    /** Job worker `create-recommendation`; always releases the item's replenishment claim on the way out. */
    @Transactional
    public UUID createRecommendation(UUID itemId, String runId) {
        try {
            InventoryItem item = inventoryItemRepository.findById(itemId)
                    .orElseThrow(() -> new NotFoundException("Inventory item not found: " + itemId));
            List<Supplier> eligibleSuppliers = supplierRepository.findAll().stream()
                    .filter(Supplier::isActive)
                    .toList();
            Map<UUID, Supplier> suppliersById =
                    eligibleSuppliers.stream().collect(Collectors.toMap(Supplier::getId, s -> s));

            RecommendationResponse response;
            try {
                response = recommendationProvider.recommend(toAiRequest(item, eligibleSuppliers));
                validateAiResponse(response, suppliersById);
            } catch (RuntimeException ex) {
                response = fallbackRecommendation(item, eligibleSuppliers);
            }

            Recommendation recommendation = new Recommendation();
            recommendation.setItemId(itemId);
            recommendation.setCurrentStock(item.getOnHandQuantity());
            recommendation.setRecommendedQuantity(response.recommendedQuantity());
            recommendation.setSupplierId(response.supplierId());
            recommendation.setTargetDeliveryDate(response.targetDeliveryDate());
            recommendation.setRiskLevel(response.riskLevel());
            recommendation.setRationale(response.rationale());
            recommendation.setStatus(RecommendationStatus.PENDING_REVIEW);
            recommendation.setCreatedAt(OffsetDateTime.now());
            recommendation = recommendationRepository.save(recommendation);
            return recommendation.getId();
        } finally {
            replenishmentApplicationService.releaseClaim(itemId, runId);
        }
    }

    /** UserTask_ReviewRecommendation completion (plan section 7). */
    @Transactional
    public Map<String, Object> recordReviewDecision(
            UUID recommendationId, String action, Map<String, Object> edits, String actingUserId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));

        switch (action) {
            case "ACCEPT" -> recommendation.setStatus(RecommendationStatus.ACCEPTED);
            case "MODIFY_AND_ACCEPT" -> {
                applyEdits(recommendation, edits);
                recommendation.setStatus(RecommendationStatus.ACCEPTED);
            }
            case "REJECT" -> recommendation.setStatus(RecommendationStatus.REJECTED);
            default -> throw new BusinessValidationException("Unsupported recommendation action " + action);
        }
        recommendation.setReviewedAt(OffsetDateTime.now());
        recommendationRepository.save(recommendation);
        return Map.of("recommendationOutcome", recommendation.getStatus().name());
    }

    /** Job worker `prepare-po-handoff` (plan section 45.3): creates the PO + outbox row at most once. */
    @Transactional
    public Map<String, Object> prepareHandoff(UUID recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));
        if (recommendation.getStatus() != RecommendationStatus.ACCEPTED) {
            throw new BusinessValidationException("Recommendation " + recommendationId + " is not accepted");
        }

        PurchaseOrder po = purchaseOrderRepository.findByRecommendationId(recommendationId)
                .orElseGet(() -> {
                    PurchaseOrder created = recommendationToPurchaseOrderMapper.toPurchaseOrder(
                            recommendation, purchaseOrderApplicationService.generatePoNumber());
                    PurchaseOrder saved = purchaseOrderRepository.save(created);
                    processStartOutboxService.enqueueCreatePurchaseOrderStart(saved);
                    return saved;
                });
        return Map.of("purchaseOrderId", po.getId().toString(), "poNumber", po.getPoNumber());
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> findAll() {
        return recommendationRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RecommendationDto getById(UUID recommendationId) {
        return recommendationRepository.findById(recommendationId)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("Recommendation not found: " + recommendationId));
    }

    private RecommendationDto toDto(Recommendation recommendation) {
        return new RecommendationDto(
                recommendation.getId(),
                recommendation.getItemId(),
                recommendation.getCurrentStock(),
                recommendation.getRecommendedQuantity(),
                recommendation.getSupplierId(),
                recommendation.getTargetDeliveryDate(),
                recommendation.getRiskLevel(),
                recommendation.getRationale(),
                recommendation.getStatus());
    }

    private void applyEdits(Recommendation recommendation, Map<String, Object> edits) {
        if (edits.containsKey("recommendedQuantity")) {
            recommendation.setRecommendedQuantity(new BigDecimal(edits.get("recommendedQuantity").toString()));
        }
        if (edits.containsKey("supplierId")) {
            UUID supplierId = UUID.fromString(edits.get("supplierId").toString());
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new BusinessValidationException("Unknown supplierId"));
            recommendation.setSupplierId(supplierId);
            recommendation.setRiskLevel(supplier.getRiskLevel());
        }
        if (edits.containsKey("targetDeliveryDate")) {
            recommendation.setTargetDeliveryDate(LocalDate.parse(edits.get("targetDeliveryDate").toString()));
        }
    }

    private RecommendationRequest toAiRequest(InventoryItem item, List<Supplier> eligibleSuppliers) {
        List<EligibleSupplier> suppliers = eligibleSuppliers.stream()
                .map(s -> new EligibleSupplier(s.getId(), s.getCode(), s.getUnitPrice(), s.getLeadTimeDays(), s.getRiskLevel()))
                .toList();
        return new RecommendationRequest(
                item.getId(), item.getSku(), item.getOnHandQuantity(), item.getReorderLevel(),
                item.getAverageConsumption(), suppliers);
    }

    private void validateAiResponse(RecommendationResponse response, Map<UUID, Supplier> suppliersById) {
        if (response.recommendedQuantity() == null || response.recommendedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("AI recommendation quantity must be positive");
        }
        Supplier supplier = suppliersById.get(response.supplierId());
        if (supplier == null || !supplier.isActive()) {
            throw new BusinessValidationException("AI recommendation references an unknown/inactive supplier");
        }
        if (response.targetDeliveryDate() == null) {
            throw new BusinessValidationException("AI recommendation target delivery date is required");
        }
        if (response.riskLevel() == null) {
            throw new BusinessValidationException("AI recommendation risk level is required");
        }
        if (response.rationale() == null || response.rationale().isBlank()) {
            throw new BusinessValidationException("AI recommendation rationale is required");
        }
    }

    /** Keeps the process usable when the AI provider is unavailable or returns an invalid response (plan section 7). */
    private RecommendationResponse fallbackRecommendation(
            InventoryItem item, List<Supplier> eligibleSuppliers) {
        Supplier fallbackSupplier = eligibleSuppliers.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessValidationException("No active suppliers available for item " + item.getSku()));
        BigDecimal quantity = item.getReorderLevel().subtract(item.getOnHandQuantity()).max(BigDecimal.ZERO);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            quantity = item.getReorderLevel();
        }
        return new RecommendationResponse(
                quantity,
                fallbackSupplier.getId(),
                LocalDate.now().plusDays(fallbackSupplier.getLeadTimeDays() + 7L),
                RiskLevel.MEDIUM,
                "Fallback recommendation: AI provider unavailable or returned invalid output; manual review required.");
    }
}
