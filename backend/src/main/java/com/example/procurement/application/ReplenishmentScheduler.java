package com.example.procurement.application;

import com.example.procurement.camunda.service.CamundaProcessService;
import java.time.Instant;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Single global batch trigger for the Replenishment Recommendation process (plan section 39.1).
 * The schedule itself is fully externalized via {@code replenishment.schedule}; this class never
 * hard-codes a production cadence. Overlapping runs are tolerated; per-item safety is the
 * {@code replenishment_claim} contract (plan section 39.3), not this trigger.
 */
@Component
public class ReplenishmentScheduler {

    private final CamundaProcessService camundaProcessService;

    public ReplenishmentScheduler(CamundaProcessService camundaProcessService) {
        this.camundaProcessService = camundaProcessService;
    }

    @Scheduled(cron = "${replenishment.schedule:0 0 * * * *}")
    public void triggerReplenishmentRun() {
        String runId = "RUN-" + Instant.now().toEpochMilli();
        camundaProcessService.startReplenishmentRecommendation(runId, Map.of("runId", runId));
    }
}
