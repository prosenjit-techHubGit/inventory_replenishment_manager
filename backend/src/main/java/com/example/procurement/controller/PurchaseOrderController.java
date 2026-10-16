package com.example.procurement.controller;

import com.example.procurement.application.PurchaseOrderApplicationService;
import com.example.procurement.application.dto.CreatePurchaseOrderRequest;
import com.example.procurement.application.dto.PurchaseOrderDetailResponse;
import com.example.procurement.application.dto.PurchaseOrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Manual PO initiation and PO read endpoints (plan sections 18, 42.1). */
@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderApplicationService purchaseOrderApplicationService;

    public PurchaseOrderController(PurchaseOrderApplicationService purchaseOrderApplicationService) {
        this.purchaseOrderApplicationService = purchaseOrderApplicationService;
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        PurchaseOrderResponse response = purchaseOrderApplicationService.createManualPurchaseOrder(
                idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public List<PurchaseOrderResponse> list() {
        return purchaseOrderApplicationService.findAll();
    }

    @GetMapping("/{id}")
    public PurchaseOrderDetailResponse getById(@PathVariable UUID id) {
        return purchaseOrderApplicationService.getById(id);
    }
}
