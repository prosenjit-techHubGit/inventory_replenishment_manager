package com.example.procurement.application.dto;

import java.util.Map;

/** Task completion request; `action` is application-level and validated against the task type (plan section 34.3). */
public record TaskCompletionRequest(String action, Map<String, Object> variables) {

    public TaskCompletionRequest {
        if (variables == null) {
            variables = Map.of();
        }
    }
}
