package com.example.procurement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Result of one claim-protected replenishment evaluation for a single item (plan sections 7, 39). */
public record ReplenishmentCheckResult(
        UUID itemId,
        BigDecimal onHandQuantity,
        BigDecimal reorderLevel,
        BigDecimal openPurchaseQuantity,
        boolean replenishmentRequired) {
}
