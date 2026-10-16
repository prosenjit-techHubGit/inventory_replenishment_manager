package com.example.procurement.repository;

import com.example.procurement.domain.shipment.Shipment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    List<Shipment> findByPurchaseOrderId(UUID purchaseOrderId);
}
