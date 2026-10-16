package com.example.procurement.domain.idempotency;

/** Status of an idempotency-tracked request (plan section 38.1). */
public enum IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    RETRYABLE_FAILURE,
    FAILED
}
