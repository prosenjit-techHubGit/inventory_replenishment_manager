package com.example.procurement.application.dto;

import com.example.procurement.domain.shipment.ShipmentStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ShipmentDto(
        UUID id,
        ShipmentStatus status,
        LocalDate plannedDeliveryDate,
        LocalDate actualDeliveryDate,
        String supplierShipmentReference,
        String trackingReference) {
}
