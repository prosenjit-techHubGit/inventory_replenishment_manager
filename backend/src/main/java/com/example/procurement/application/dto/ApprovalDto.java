package com.example.procurement.application.dto;

import com.example.procurement.domain.purchaseorder.ApprovalOutcome;
import com.example.procurement.domain.purchaseorder.ApprovalRole;
import java.util.UUID;

public record ApprovalDto(
        UUID id,
        int poRevision,
        ApprovalRole approvalRole,
        ApprovalOutcome outcome,
        String approverUserId,
        String comment,
        String completedAt) {
}
