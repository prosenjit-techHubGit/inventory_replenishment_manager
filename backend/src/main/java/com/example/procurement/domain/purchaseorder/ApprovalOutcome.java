package com.example.procurement.domain.purchaseorder;

/** Outcome of a single approval task (plan section 35.4). */
public enum ApprovalOutcome {
    APPROVED,
    RETURNED_FOR_REVISION,
    REJECTED
}
