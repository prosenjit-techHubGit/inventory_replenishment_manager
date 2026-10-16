package com.example.procurement.application.dto;

import java.time.OffsetDateTime;

/** Work-queue row returned by GET /api/v1/tasks (plan section 42.2). */
public record TaskSummaryResponse(
        String taskKey,
        String name,
        String elementId,
        String bpmnProcessId,
        String assignee,
        String businessId,
        OffsetDateTime creationDate) {
}
