package com.example.procurement.exception;

/** Thrown when a request fails business validation that the controller layer must map to HTTP 400. */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
