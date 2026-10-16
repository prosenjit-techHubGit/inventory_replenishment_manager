package com.example.procurement.camunda.service;

import com.example.procurement.camunda.ProcessConstants;
import com.example.procurement.exception.ProcessIntegrationException;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Thin wrapper around {@link CamundaClient} for starting processes and publishing messages (plan section 16/21/44). */
@Service
public class CamundaProcessService {

    private final CamundaClient camundaClient;
    private final Duration createPurchaseOrderMessageTtl;

    public CamundaProcessService(
            CamundaClient camundaClient,
            @Value("${camunda.message.create-po-ttl:PT1H}") Duration createPurchaseOrderMessageTtl) {
        this.camundaClient = camundaClient;
        this.createPurchaseOrderMessageTtl = createPurchaseOrderMessageTtl;
    }

    /**
     * Publishes the {@code CREATE_PURCHASE_ORDER} message start event (plan sections 44.1/44.2).
     * {@code messageId} is the stable outbox event id (dedupes republished retries); {@code
     * correlationKey} is the {@code poRequestId} (dedupes concurrently active instances).
     */
    public void publishCreatePurchaseOrderMessage(String messageId, String correlationKey, Map<String, Object> variables) {
        try {
            camundaClient.newPublishMessageCommand()
                    .messageName(ProcessConstants.MESSAGE_CREATE_PURCHASE_ORDER)
                    .correlationKey(correlationKey)
                    .messageId(messageId)
                    .timeToLive(createPurchaseOrderMessageTtl)
                    .variables(variables)
                    .execute();
        } catch (RuntimeException ex) {
            throw new ProcessIntegrationException(
                    "Failed to publish message " + ProcessConstants.MESSAGE_CREATE_PURCHASE_ORDER
                            + " for correlationKey " + correlationKey,
                    ex);
        }
    }

    /** Starts the Replenishment Recommendation batch process (plan sections 7, 39.1). */
    public ProcessInstanceEvent startReplenishmentRecommendation(String businessId, Map<String, Object> variables) {
        try {
            return camundaClient.newCreateInstanceCommand()
                    .bpmnProcessId(ProcessConstants.REPLENISHMENT_RECOMMENDATION_PROCESS_ID)
                    .latestVersion()
                    .variables(variables)
                    .businessId(businessId)
                    .execute();
        } catch (RuntimeException ex) {
            throw new ProcessIntegrationException(
                    "Failed to start process " + ProcessConstants.REPLENISHMENT_RECOMMENDATION_PROCESS_ID
                            + " for businessId " + businessId,
                    ex);
        }
    }
}
