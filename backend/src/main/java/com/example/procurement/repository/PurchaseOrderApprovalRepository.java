package com.example.procurement.repository;

import com.example.procurement.domain.purchaseorder.ApprovalRole;
import com.example.procurement.domain.purchaseorder.PurchaseOrderApproval;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderApprovalRepository extends JpaRepository<PurchaseOrderApproval, UUID> {

    List<PurchaseOrderApproval> findByPurchaseOrderIdOrderByPoRevisionAscCreatedAtAsc(UUID purchaseOrderId);

    List<PurchaseOrderApproval> findByPurchaseOrderIdAndPoRevision(UUID purchaseOrderId, int poRevision);

    Optional<PurchaseOrderApproval> findByPurchaseOrderIdAndPoRevisionAndApprovalRole(
            UUID purchaseOrderId, int poRevision, ApprovalRole approvalRole);
}
