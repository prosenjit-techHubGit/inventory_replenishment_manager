package com.example.procurement.domain.outbox;

/** Lifecycle of a {@link ProcessStartOutbox} event (plan section 46.1). */
public enum OutboxStatus {
    PENDING,
    PUBLISHING,
    PUBLISHED,
    RETRYABLE_FAILURE,
    DEAD_LETTER
}
