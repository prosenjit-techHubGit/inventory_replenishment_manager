package com.example.procurement.domain.purchaseorder;

/** Full purchase-order lifecycle (plan section 37.2). */
public enum PurchaseOrderStatus {
    DRAFT,
    PENDING_PROCESS_START,
    IN_PREPARATION,
    PENDING_APPROVAL,
    REVISION_REQUIRED,
    REJECTED,
    APPROVED,
    PENDING_SUPPLIER_CONFIRMATION,
    SUPPLIER_REJECTED,
    SUPPLIER_DATE_REVISED,
    SUPPLIER_CONFIRMED,
    RELEASED,
    IN_FULFILLMENT,
    PARTIALLY_RECEIVED,
    RECEIVED,
    COMPLETED,
    CANCELLED,
    START_FAILED
}
