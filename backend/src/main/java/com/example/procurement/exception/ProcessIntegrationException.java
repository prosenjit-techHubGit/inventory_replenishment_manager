package com.example.procurement.exception;

/** Thrown when Camunda process/task integration fails in a way the caller must retry (maps to HTTP 502/503). */
public class ProcessIntegrationException extends RuntimeException {

    public ProcessIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessIntegrationException(String message) {
        super(message);
    }
}
