package com.example.procurement.exception;

/** Thrown when an Idempotency-Key is reused with a different request body (maps to HTTP 409). */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
