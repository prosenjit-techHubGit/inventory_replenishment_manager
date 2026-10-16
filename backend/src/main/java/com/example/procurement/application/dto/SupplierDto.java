package com.example.procurement.application.dto;

import com.example.procurement.domain.RiskLevel;
import java.math.BigDecimal;
import java.util.UUID;

/** Catalog row for supplier dropdowns and recommendation context. */
public record SupplierDto(
        UUID id,
        String code,
        String name,
        boolean active,
        BigDecimal unitPrice,
        int leadTimeDays,
        BigDecimal deliveryScore,
        BigDecimal qualityScore,
        RiskLevel riskLevel) {
}
