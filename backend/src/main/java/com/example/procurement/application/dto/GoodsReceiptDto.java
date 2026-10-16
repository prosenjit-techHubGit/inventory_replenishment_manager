package com.example.procurement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoodsReceiptDto(
        UUID id,
        int receiptNumber,
        BigDecimal receivedQuantity,
        BigDecimal acceptedQuantity,
        BigDecimal rejectedQuantity,
        LocalDate receiptDate,
        String notes) {
}
