package com.example.procurement.application.dto;

import java.util.List;
import java.util.Map;

/** Task detail returned by GET /api/v1/tasks/{taskKey} (plan section 42.3). */
public record TaskDetailResponse(
        String taskKey,
        String taskType,
        String name,
        String businessId,
        List<String> allowedActions,
        Map<String, Object> data) {
}
