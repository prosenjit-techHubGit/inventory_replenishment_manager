package com.example.procurement.controller;

import com.example.procurement.application.CatalogApplicationService;
import com.example.procurement.application.dto.InventoryItemDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final CatalogApplicationService catalogApplicationService;

    public InventoryController(CatalogApplicationService catalogApplicationService) {
        this.catalogApplicationService = catalogApplicationService;
    }

    @GetMapping
    public List<InventoryItemDto> list() {
        return catalogApplicationService.listInventory();
    }
}
