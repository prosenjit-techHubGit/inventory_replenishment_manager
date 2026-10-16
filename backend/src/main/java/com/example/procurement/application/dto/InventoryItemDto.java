package com.example.procurement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Catalog row for inventory dropdowns and dashboard watch list. */
public record InventoryItemDto(
        UUID id,
        String sku,
        String name,
        BigDecimal onHandQuantity,
        BigDecimal reorderLevel,
        BigDecimal openPurchaseQuantity,
        BigDecimal projectedAvailable,
        boolean criticalItem,
        boolean replenishmentRequired) {
}
