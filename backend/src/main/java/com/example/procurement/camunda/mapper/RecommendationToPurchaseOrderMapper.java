package com.example.procurement.camunda.mapper;

import com.example.procurement.domain.RiskLevel;
import com.example.procurement.domain.Urgency;
import com.example.procurement.domain.purchaseorder.InitiationType;
import com.example.procurement.domain.purchaseorder.PurchaseOrder;
import com.example.procurement.domain.purchaseorder.PurchaseOrderStatus;
import com.example.procurement.domain.recommendation.Recommendation;
import com.example.procurement.domain.supplier.Supplier;
import com.example.procurement.exception.BusinessValidationException;
import com.example.procurement.repository.SupplierRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/** Maps an accepted {@link Recommendation} into prepopulated PO fields (plan section 7). */
@Component
public class RecommendationToPurchaseOrderMapper {

    private final SupplierRepository supplierRepository;

    public RecommendationToPurchaseOrderMapper(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public PurchaseOrder toPurchaseOrder(Recommendation recommendation, String poNumber) {
        Supplier supplier = supplierRepository.findById(recommendation.getSupplierId())
                .orElseThrow(() -> new BusinessValidationException(
                        "Unknown supplierId on recommendation " + recommendation.getId()));

        OffsetDateTime now = OffsetDateTime.now();
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(poNumber);
        po.setPoRevision(1);
        po.setInitiationType(InitiationType.REPLENISHMENT);
        po.setRecommendationId(recommendation.getId());
        po.setItemId(recommendation.getItemId());
        po.setSupplierId(recommendation.getSupplierId());
        po.setQuantity(recommendation.getRecommendedQuantity());
        po.setUnitPrice(supplier.getUnitPrice());
        po.setTotalValue(recommendation.getRecommendedQuantity().multiply(supplier.getUnitPrice()));
        po.setRequiredDeliveryDate(recommendation.getTargetDeliveryDate());
        po.setUrgency(recommendation.getRiskLevel() == RiskLevel.HIGH ? Urgency.URGENT : Urgency.NORMAL);
        po.setSupplierRisk(recommendation.getRiskLevel());
        po.setBusinessJustification(
                "Auto-generated from accepted recommendation " + recommendation.getId() + ": " + recommendation.getRationale());
        po.setStatus(PurchaseOrderStatus.PENDING_PROCESS_START);
        po.setPoRequestId("REC:" + recommendation.getId());
        po.setCreatedAt(now);
        po.setUpdatedAt(now);
        return po;
    }
}
