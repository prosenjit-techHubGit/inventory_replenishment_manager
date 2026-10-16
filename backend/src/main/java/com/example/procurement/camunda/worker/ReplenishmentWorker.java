package com.example.procurement.camunda.worker;

import com.example.procurement.application.ReplenishmentApplicationService;
import com.example.procurement.application.RecommendationApplicationService;
import com.example.procurement.application.dto.ReplenishmentCheckResult;
import com.example.procurement.camunda.ProcessConstants;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Job workers for the Replenishment Recommendation process (plan section 7); thin, delegates to application services. */
@Component
public class ReplenishmentWorker {

    private final ReplenishmentApplicationService replenishmentApplicationService;
    private final RecommendationApplicationService recommendationApplicationService;

    public ReplenishmentWorker(
            ReplenishmentApplicationService replenishmentApplicationService,
            RecommendationApplicationService recommendationApplicationService) {
        this.replenishmentApplicationService = replenishmentApplicationService;
        this.recommendationApplicationService = recommendationApplicationService;
    }

    @JobWorker(type = ProcessConstants.JOB_LIST_REPLENISHMENT_CANDIDATES)
    public Map<String, Object> listCandidates() {
        List<String> candidateItemIds = replenishmentApplicationService.listCandidateItemIds().stream()
                .map(UUID::toString)
                .toList();
        return Map.of("candidateItemIds", candidateItemIds);
    }

    @JobWorker(type = ProcessConstants.JOB_CHECK_REPLENISHMENT)
    public Map<String, Object> checkReplenishment(
            @Variable(name = "itemId") String itemId, @Variable(name = "runId") String runId) {
        ReplenishmentCheckResult result = replenishmentApplicationService.checkReplenishment(
                UUID.fromString(itemId), runId);
        Map<String, Object> variables = new HashMap<>();
        variables.put("onHandQuantity", result.onHandQuantity());
        variables.put("reorderLevel", result.reorderLevel());
        variables.put("openPurchaseQuantity", result.openPurchaseQuantity());
        variables.put("replenishmentRequired", result.replenishmentRequired());
        return variables;
    }

    @JobWorker(type = ProcessConstants.JOB_CREATE_RECOMMENDATION)
    public Map<String, Object> createRecommendation(
            @Variable(name = "itemId") String itemId, @Variable(name = "runId") String runId) {
        UUID recommendationId = recommendationApplicationService.createRecommendation(UUID.fromString(itemId), runId);
        return Map.of("recommendationId", recommendationId.toString());
    }

    @JobWorker(type = ProcessConstants.JOB_PREPARE_PO_HANDOFF)
    public Map<String, Object> prepareHandoff(@Variable(name = "recommendationId") String recommendationId) {
        return recommendationApplicationService.prepareHandoff(UUID.fromString(recommendationId));
    }
}
