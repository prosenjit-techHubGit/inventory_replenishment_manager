package com.example.procurement.camunda.worker;

import com.example.procurement.application.ShipmentApplicationService;
import com.example.procurement.camunda.ProcessConstants;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShipmentWorker {

    private final ShipmentApplicationService shipmentApplicationService;

    public ShipmentWorker(ShipmentApplicationService shipmentApplicationService) {
        this.shipmentApplicationService = shipmentApplicationService;
    }

    /** Triggered by the non-interrupting overdue timer boundary event on Track Shipment (plan section 13). */
    @JobWorker(type = ProcessConstants.JOB_FLAG_SHIPMENT_DELAYED)
    public void flagDelayed(@Variable(name = "purchaseOrderId") String purchaseOrderId) {
        shipmentApplicationService.flagDelayed(UUID.fromString(purchaseOrderId));
    }
}
