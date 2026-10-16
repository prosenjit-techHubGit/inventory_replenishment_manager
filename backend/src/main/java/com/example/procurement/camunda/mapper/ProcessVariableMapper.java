package com.example.procurement.camunda.mapper;

import com.example.procurement.domain.purchaseorder.PurchaseOrder;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps a {@link PurchaseOrder} to the small, intentional set of Camunda process variables
 * (plan section 22) — never serialize whole JPA entities into process variables.
 */
@Component
public class ProcessVariableMapper {

    public Map<String, Object> toCreatePurchaseOrderVariables(PurchaseOrder po, boolean criticalItem) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("purchaseOrderId", po.getId().toString());
        variables.put("poNumber", po.getPoNumber());
        variables.put("poRevision", po.getPoRevision());
        variables.put("poRequestId", po.getPoRequestId());
        variables.put("initiationType", po.getInitiationType().name());
        variables.put("recommendationId", po.getRecommendationId() == null ? null : po.getRecommendationId().toString());
        variables.put("itemId", po.getItemId().toString());
        variables.put("supplierId", po.getSupplierId().toString());
        variables.put("quantity", po.getQuantity());
        variables.put("unitPrice", po.getUnitPrice());
        variables.put("poValue", po.getTotalValue());
        variables.put("requiredDeliveryDate", po.getRequiredDeliveryDate().toString());
        variables.put("urgency", po.getUrgency().name());
        variables.put("supplierRisk", po.getSupplierRisk().name());
        variables.put("criticalItem", criticalItem);
        return variables;
    }
}
