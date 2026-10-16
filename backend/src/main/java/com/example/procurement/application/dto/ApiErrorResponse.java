package com.example.procurement.application.dto;

import java.time.OffsetDateTime;

/** Uniform error body returned by {@code ApiExceptionHandler}. */
public record ApiErrorResponse(String code, String message, OffsetDateTime timestamp) {

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, OffsetDateTime.now());
    }
}
