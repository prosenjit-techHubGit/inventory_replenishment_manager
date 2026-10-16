package com.example.procurement.exception;

/** Thrown when a referenced id (task, PO, item, supplier) does not exist (maps to HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
