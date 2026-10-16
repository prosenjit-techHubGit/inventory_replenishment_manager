package com.example.procurement.ai;

import com.example.procurement.domain.RiskLevel;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Structured input to a {@link RecommendationProvider} (plan sections 7, 17). */
public record RecommendationRequest(
        UUID itemId,
        String sku,
        BigDecimal onHandQuantity,
        BigDecimal reorderLevel,
        BigDecimal averageConsumption,
        List<EligibleSupplier> eligibleSuppliers) {

    public record EligibleSupplier(
            UUID supplierId, String code, BigDecimal unitPrice, int leadTimeDays, RiskLevel riskLevel) {
    }
}
