package com.example.procurement.camunda.worker;

import com.example.procurement.application.InventoryApplicationService;
import com.example.procurement.camunda.ProcessConstants;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InventoryWorker {

    private final InventoryApplicationService inventoryApplicationService;

    public InventoryWorker(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    /** Idempotent by goodsReceiptId (plan section 39.4); returns fullyReceived for the BPMN gateway. */
    @JobWorker(type = ProcessConstants.JOB_UPDATE_INVENTORY)
    public Map<String, Object> updateInventory(@Variable(name = "goodsReceiptId") String goodsReceiptId) {
        boolean fullyReceived = inventoryApplicationService.applyGoodsReceipt(UUID.fromString(goodsReceiptId));
        return Map.of("fullyReceived", fullyReceived);
    }
}
