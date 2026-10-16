package com.example.procurement.application.dto;

import com.example.procurement.domain.Urgency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Manual Create PO form submission (plan section 9 / 42.1). */
public record CreatePurchaseOrderRequest(
        @NotBlank String itemId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal quantity,
        @NotBlank String supplierId,
        @NotNull @DecimalMin(value = "0.00") BigDecimal unitPrice,
        @NotNull @FutureOrPresent LocalDate requiredDeliveryDate,
        @NotNull Urgency urgency,
        String businessJustification) {
}
