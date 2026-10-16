package com.example.procurement.application;

import com.example.procurement.application.dto.InventoryItemDto;
import com.example.procurement.application.dto.SupplierDto;
import com.example.procurement.domain.inventory.InventoryItem;
import com.example.procurement.repository.InventoryItemRepository;
import com.example.procurement.repository.SupplierRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only catalog used by React forms, dashboard watch list, and task screens. */
@Service
public class CatalogApplicationService {

    private final InventoryItemRepository inventoryItemRepository;
    private final SupplierRepository supplierRepository;

    public CatalogApplicationService(
            InventoryItemRepository inventoryItemRepository, SupplierRepository supplierRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> listInventory() {
        return inventoryItemRepository.findAll().stream().map(this::toItemDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> listSuppliers() {
        return supplierRepository.findAll().stream()
                .map(s -> new SupplierDto(
                        s.getId(),
                        s.getCode(),
                        s.getName(),
                        s.isActive(),
                        s.getUnitPrice(),
                        s.getLeadTimeDays(),
                        s.getDeliveryScore(),
                        s.getQualityScore(),
                        s.getRiskLevel()))
                .toList();
    }

    private InventoryItemDto toItemDto(InventoryItem item) {
        BigDecimal open = item.getOpenPurchaseQuantity() == null ? BigDecimal.ZERO : item.getOpenPurchaseQuantity();
        BigDecimal projected = item.getOnHandQuantity().add(open);
        boolean replenishmentRequired = projected.compareTo(item.getReorderLevel()) < 0;
        return new InventoryItemDto(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getOnHandQuantity(),
                item.getReorderLevel(),
                open,
                projected,
                item.isCriticalItem(),
                replenishmentRequired);
    }
}
