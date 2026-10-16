package com.example.procurement.domain.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "on_hand_quantity", nullable = false)
    private BigDecimal onHandQuantity;

    @Column(name = "reorder_level", nullable = false)
    private BigDecimal reorderLevel;

    @Column(name = "average_consumption", nullable = false)
    private BigDecimal averageConsumption;

    @Column(name = "open_purchase_quantity", nullable = false)
    private BigDecimal openPurchaseQuantity;

    @Column(name = "critical_item", nullable = false)
    private boolean criticalItem;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private Long version;

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getOnHandQuantity() {
        return onHandQuantity;
    }

    public void setOnHandQuantity(BigDecimal onHandQuantity) {
        this.onHandQuantity = onHandQuantity;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getAverageConsumption() {
        return averageConsumption;
    }

    public void setAverageConsumption(BigDecimal averageConsumption) {
        this.averageConsumption = averageConsumption;
    }

    public BigDecimal getOpenPurchaseQuantity() {
        return openPurchaseQuantity;
    }

    public void setOpenPurchaseQuantity(BigDecimal openPurchaseQuantity) {
        this.openPurchaseQuantity = openPurchaseQuantity;
    }

    public boolean isCriticalItem() {
        return criticalItem;
    }

    public void setCriticalItem(boolean criticalItem) {
        this.criticalItem = criticalItem;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
