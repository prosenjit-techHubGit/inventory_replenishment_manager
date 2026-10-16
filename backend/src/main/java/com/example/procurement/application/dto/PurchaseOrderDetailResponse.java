package com.example.procurement.application.dto;

import java.math.BigDecimal;
import java.util.List;

/** PO detail for the React status screen: facts plus shipments, receipts and approvals. */
public record PurchaseOrderDetailResponse(
        PurchaseOrderResponse purchaseOrder,
        String itemSku,
        String itemName,
        boolean criticalItem,
        String supplierCode,
        String supplierName,
        List<ShipmentDto> shipments,
        List<GoodsReceiptDto> receipts,
        List<ApprovalDto> approvals,
        BigDecimal cumulativeAcceptedQuantity,
        BigDecimal remainingQuantity) {
}
