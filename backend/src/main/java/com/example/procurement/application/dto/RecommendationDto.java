package com.example.procurement.application.dto;

import com.example.procurement.domain.RiskLevel;
import com.example.procurement.domain.recommendation.RecommendationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Read-only representation of a Recommendation (plan section 18). */
public record RecommendationDto(
        UUID recommendationId,
        UUID itemId,
        BigDecimal currentStock,
        BigDecimal recommendedQuantity,
        UUID supplierId,
        LocalDate targetDeliveryDate,
        RiskLevel riskLevel,
        String rationale,
        RecommendationStatus status) {
}
