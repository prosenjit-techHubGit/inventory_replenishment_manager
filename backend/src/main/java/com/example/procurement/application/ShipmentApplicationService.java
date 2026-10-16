package com.example.procurement.application;

import com.example.procurement.domain.shipment.Shipment;
import com.example.procurement.domain.shipment.ShipmentStatus;
import com.example.procurement.exception.NotFoundException;
import com.example.procurement.repository.ShipmentRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shipment tracking and overdue escalation (plan section 13). */
@Service
public class ShipmentApplicationService {

    private final ShipmentRepository shipmentRepository;

    public ShipmentApplicationService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    /** Job worker `flag-shipment-delayed`, triggered by the non-interrupting overdue timer. */
    @Transactional
    public void flagDelayed(UUID purchaseOrderId) {
        Shipment shipment = latestShipment(purchaseOrderId);
        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            shipment.setStatus(ShipmentStatus.DELAYED);
            shipment.setUpdatedAt(OffsetDateTime.now());
            shipmentRepository.save(shipment);
        }
    }

    /** UserTask_TrackShipment completion: records the delivery. */
    @Transactional
    public void recordDelivery(UUID purchaseOrderId, LocalDate actualDeliveryDate, String trackingReference) {
        Shipment shipment = latestShipment(purchaseOrderId);
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setActualDeliveryDate(actualDeliveryDate != null ? actualDeliveryDate : LocalDate.now());
        if (trackingReference != null && !trackingReference.isBlank()) {
            shipment.setTrackingReference(trackingReference);
        }
        shipment.setUpdatedAt(OffsetDateTime.now());
        shipmentRepository.save(shipment);
    }

    private Shipment latestShipment(UUID purchaseOrderId) {
        return shipmentRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .max(Comparator.comparing(Shipment::getUpdatedAt))
                .orElseThrow(() -> new NotFoundException("No shipment found for purchase order " + purchaseOrderId));
    }
}
