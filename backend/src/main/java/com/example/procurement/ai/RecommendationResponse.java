package com.example.procurement.ai;

import com.example.procurement.domain.RiskLevel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Structured output of a {@link RecommendationProvider}, validated before persistence (plan section 7). */
public record RecommendationResponse(
        BigDecimal recommendedQuantity,
        UUID supplierId,
        LocalDate targetDeliveryDate,
        RiskLevel riskLevel,
        String rationale) {
}
