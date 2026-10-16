package com.example.procurement.repository;

import com.example.procurement.domain.receipt.GoodsReceipt;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

    List<GoodsReceipt> findByPurchaseOrderIdOrderByReceiptNumber(UUID purchaseOrderId);

    Optional<GoodsReceipt> findByPurchaseOrderIdAndReceiptNumber(UUID purchaseOrderId, int receiptNumber);

    @Query("select coalesce(sum(g.acceptedQuantity), 0) from GoodsReceipt g "
            + "where g.purchaseOrderId = :purchaseOrderId and g.appliedAt is not null")
    BigDecimal sumAppliedAcceptedQuantity(@Param("purchaseOrderId") UUID purchaseOrderId);
}
