package com.example.procurement.repository;

import com.example.procurement.domain.purchaseorder.PurchaseOrder;
import com.example.procurement.domain.purchaseorder.PurchaseOrderStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    Optional<PurchaseOrder> findByPoRequestId(String poRequestId);

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    Optional<PurchaseOrder> findByRecommendationId(UUID recommendationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PurchaseOrder p where p.id = :id")
    Optional<PurchaseOrder> lockById(@Param("id") UUID id);

    @Query(value = "select nextval('po_number_seq')", nativeQuery = true)
    long nextPoNumberSequenceValue();

    /** Incoming-supply remainder across qualifying (not-yet-fully-settled) POs for an item (plan section 49.2). */
    @Query("select coalesce(sum(p.quantity - coalesce("
            + "(select sum(g.acceptedQuantity) from GoodsReceipt g where g.purchaseOrderId = p.id and g.appliedAt is not null), 0)), 0) "
            + "from PurchaseOrder p where p.itemId = :itemId and p.status in :statuses")
    BigDecimal sumOpenPurchaseQuantity(@Param("itemId") UUID itemId, @Param("statuses") List<PurchaseOrderStatus> statuses);

    /** Duplicate-start reconciliation (plan section 46.4): claims the PO for this process instance only once. */
    @Modifying
    @Query(value = "UPDATE purchase_order SET camunda_process_instance_key = :processInstanceKey, "
            + "status = 'IN_PREPARATION', updated_at = now() "
            + "WHERE id = :id AND camunda_process_instance_key IS NULL", nativeQuery = true)
    int registerProcessInstanceKeyIfAbsent(@Param("id") UUID id, @Param("processInstanceKey") String processInstanceKey);
}
