package com.example.procurement.domain.purchaseorder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** One row per required approval role for a given PO revision (plan section 35.5). */
@Entity
@Table(name = "purchase_order_approval")
public class PurchaseOrderApproval {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "purchase_order_id", nullable = false)
    private UUID purchaseOrderId;

    @Column(name = "po_revision", nullable = false)
    private int poRevision;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_role", nullable = false)
    private ApprovalRole approvalRole;

    @Column(name = "approver_user_id")
    private String approverUserId;

    @Enumerated(EnumType.STRING)
    @Column
    private ApprovalOutcome outcome;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public UUID getId() {
        return id;
    }

    public UUID getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(UUID purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public int getPoRevision() {
        return poRevision;
    }

    public void setPoRevision(int poRevision) {
        this.poRevision = poRevision;
    }

    public ApprovalRole getApprovalRole() {
        return approvalRole;
    }

    public void setApprovalRole(ApprovalRole approvalRole) {
        this.approvalRole = approvalRole;
    }

    public String getApproverUserId() {
        return approverUserId;
    }

    public void setApproverUserId(String approverUserId) {
        this.approverUserId = approverUserId;
    }

    public ApprovalOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(ApprovalOutcome outcome) {
        this.outcome = outcome;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
