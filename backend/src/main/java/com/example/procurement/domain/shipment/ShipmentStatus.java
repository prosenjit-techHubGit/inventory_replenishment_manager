package com.example.procurement.domain.shipment;

/** Shipment lifecycle (plan section 37.3). */
public enum ShipmentStatus {
    PLANNED,
    DISPATCHED,
    IN_TRANSIT,
    DELAYED,
    DELIVERED,
    CANCELLED
}
