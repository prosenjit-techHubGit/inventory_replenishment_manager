package com.example.procurement.repository;

import com.example.procurement.domain.inventory.InventoryItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findBySku(String sku);

    /** Cheap pre-filter for the replenishment batch (plan section 39.1); claim + fresh re-read decide the real outcome. */
    @Query("select i.id from InventoryItem i where i.onHandQuantity <= i.reorderLevel")
    List<UUID> findCandidateItemIds();
}
