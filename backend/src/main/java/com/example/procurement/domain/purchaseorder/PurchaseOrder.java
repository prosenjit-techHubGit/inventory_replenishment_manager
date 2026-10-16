package com.example.procurement.domain.purchaseorder;

import com.example.procurement.domain.RiskLevel;
import com.example.procurement.domain.Urgency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "po_number", nullable = false, unique = true)
    private String poNumber;

    @Column(name = "po_revision", nullable = false)
    private int poRevision = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiation_type", nullable = false)
    private InitiationType initiationType;

    @Column(name = "recommendation_id")
    private UUID recommendationId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_value", nullable = false)
    private BigDecimal totalValue;

    @Column(name = "required_delivery_date", nullable = false)
    private LocalDate requiredDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_risk", nullable = false)
    private RiskLevel supplierRisk;

    @Column(name = "business_justification", columnDefinition = "text")
    private String businessJustification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus status;

    /** Deterministic correlation id used for idempotent process startup (plan section 38). */
    @Column(name = "po_request_id", nullable = false, unique = true)
    private String poRequestId;

    @Column(name = "camunda_process_instance_key")
    private String camundaProcessInstanceKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public int getPoRevision() {
        return poRevision;
    }

    public void setPoRevision(int poRevision) {
        this.poRevision = poRevision;
    }

    public InitiationType getInitiationType() {
        return initiationType;
    }

    public void setInitiationType(InitiationType initiationType) {
        this.initiationType = initiationType;
    }

    public UUID getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(UUID recommendationId) {
        this.recommendationId = recommendationId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public LocalDate getRequiredDeliveryDate() {
        return requiredDeliveryDate;
    }

    public void setRequiredDeliveryDate(LocalDate requiredDeliveryDate) {
        this.requiredDeliveryDate = requiredDeliveryDate;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public RiskLevel getSupplierRisk() {
        return supplierRisk;
    }

    public void setSupplierRisk(RiskLevel supplierRisk) {
        this.supplierRisk = supplierRisk;
    }

    public String getBusinessJustification() {
        return businessJustification;
    }

    public void setBusinessJustification(String businessJustification) {
        this.businessJustification = businessJustification;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseOrderStatus status) {
        this.status = status;
    }

    public String getPoRequestId() {
        return poRequestId;
    }

    public void setPoRequestId(String poRequestId) {
        this.poRequestId = poRequestId;
    }

    public String getCamundaProcessInstanceKey() {
        return camundaProcessInstanceKey;
    }

    public void setCamundaProcessInstanceKey(String camundaProcessInstanceKey) {
        this.camundaProcessInstanceKey = camundaProcessInstanceKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
