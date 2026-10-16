package com.example.procurement.application;

import com.example.procurement.domain.inventory.InventoryItem;
import com.example.procurement.domain.purchaseorder.PurchaseOrder;
import com.example.procurement.domain.purchaseorder.PurchaseOrderStatus;
import com.example.procurement.domain.receipt.GoodsReceipt;
import com.example.procurement.exception.BusinessValidationException;
import com.example.procurement.exception.NotFoundException;
import com.example.procurement.repository.GoodsReceiptRepository;
import com.example.procurement.repository.InventoryItemRepository;
import com.example.procurement.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Goods receipt capture and idempotent inventory application (plan sections 14, 15, 37.4, 39.4). */
@Service
public class InventoryApplicationService {

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public InventoryApplicationService(
            GoodsReceiptRepository goodsReceiptRepository,
            InventoryItemRepository inventoryItemRepository,
            PurchaseOrderRepository purchaseOrderRepository) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    /** UserTask_GoodsReceipt completion: persists the immutable receipt event. Returns its id. */
    @Transactional
    public UUID recordGoodsReceipt(UUID purchaseOrderId, Map<String, Object> data) {
        PurchaseOrder po = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + purchaseOrderId));

        BigDecimal received = toBigDecimal(data.get("receivedQuantity"), "receivedQuantity");
        BigDecimal accepted = toBigDecimal(data.get("acceptedQuantity"), "acceptedQuantity");
        BigDecimal rejected = data.get("rejectedQuantity") != null
                ? toBigDecimal(data.get("rejectedQuantity"), "rejectedQuantity")
                : received.subtract(accepted);
        if (accepted.add(rejected).compareTo(received) > 0) {
            throw new BusinessValidationException("accepted + rejected quantity cannot exceed received quantity");
        }

        int nextReceiptNumber = goodsReceiptRepository.findByPurchaseOrderIdOrderByReceiptNumber(purchaseOrderId).size() + 1;
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setPurchaseOrderId(purchaseOrderId);
        receipt.setReceiptNumber(nextReceiptNumber);
        receipt.setReceivedQuantity(received);
        receipt.setAcceptedQuantity(accepted);
        receipt.setRejectedQuantity(rejected);
        receipt.setReceiptDate(data.get("receiptDate") != null
                ? LocalDate.parse(data.get("receiptDate").toString())
                : LocalDate.now());
        receipt.setNotes(data.get("notes") != null ? data.get("notes").toString() : null);
        receipt.setCreatedAt(OffsetDateTime.now());
        receipt = goodsReceiptRepository.save(receipt);
        return receipt.getId();
    }

    /**
     * Job worker `update-inventory`. Idempotent by {@code goodsReceiptId}: a worker retry must
     * not add inventory twice (plan section 39.4). Returns whether the PO is now fully received.
     */
    @Transactional
    public boolean applyGoodsReceipt(UUID goodsReceiptId) {
        GoodsReceipt receipt = goodsReceiptRepository.findById(goodsReceiptId)
                .orElseThrow(() -> new NotFoundException("Goods receipt not found: " + goodsReceiptId));
        PurchaseOrder po = purchaseOrderRepository.findById(receipt.getPurchaseOrderId())
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + receipt.getPurchaseOrderId()));

        if (receipt.getAppliedAt() == null) {
            InventoryItem item = inventoryItemRepository.findById(po.getItemId())
                    .orElseThrow(() -> new NotFoundException("Inventory item not found: " + po.getItemId()));
            item.setOnHandQuantity(item.getOnHandQuantity().add(receipt.getAcceptedQuantity()));
            item.setUpdatedAt(OffsetDateTime.now());
            inventoryItemRepository.save(item);

            receipt.setAppliedAt(OffsetDateTime.now());
            goodsReceiptRepository.save(receipt);
        }

        BigDecimal cumulativeAccepted = goodsReceiptRepository.sumAppliedAcceptedQuantity(po.getId());
        boolean fullyReceived = cumulativeAccepted.compareTo(po.getQuantity()) >= 0;
        po.setStatus(fullyReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);
        return fullyReceived;
    }

    private BigDecimal toBigDecimal(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessValidationException(fieldName + " is required");
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw new BusinessValidationException("Invalid " + fieldName + ": " + value);
        }
    }
}
