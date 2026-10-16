package com.example.procurement.application.dto;

import com.example.procurement.domain.purchaseorder.InitiationType;
import com.example.procurement.domain.purchaseorder.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Public representation of a purchase order (plan section 42.1). */
public record PurchaseOrderResponse(
        UUID purchaseOrderId,
        String poNumber,
        int poRevision,
        PurchaseOrderStatus status,
        InitiationType initiationType,
        String processInstanceKey,
        UUID itemId,
        UUID supplierId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalValue,
        LocalDate requiredDeliveryDate) {
}
