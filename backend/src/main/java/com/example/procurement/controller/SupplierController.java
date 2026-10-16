package com.example.procurement.controller;

import com.example.procurement.application.CatalogApplicationService;
import com.example.procurement.application.dto.SupplierDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final CatalogApplicationService catalogApplicationService;

    public SupplierController(CatalogApplicationService catalogApplicationService) {
        this.catalogApplicationService = catalogApplicationService;
    }

    @GetMapping
    public List<SupplierDto> list() {
        return catalogApplicationService.listSuppliers();
    }
}
