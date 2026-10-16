package com.example.procurement.camunda.service;

import com.example.procurement.domain.outbox.OutboxStatus;
import com.example.procurement.domain.outbox.ProcessStartOutbox;
import com.example.procurement.repository.ProcessStartOutboxRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background publisher for the transactional outbox (plan section 46.2/46.3). Retries reuse the
 * same outbox row id as the Camunda message id, so republishing is safe.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int MAX_ATTEMPTS = 10;

    private final ProcessStartOutboxRepository outboxRepository;
    private final CamundaProcessService camundaProcessService;
    // Spring Boot 4 autoconfigures a Jackson 3 JsonMapper, not this classic ObjectMapper; construct our own.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutboxPublisher(
            ProcessStartOutboxRepository outboxRepository,
            CamundaProcessService camundaProcessService) {
        this.outboxRepository = outboxRepository;
        this.camundaProcessService = camundaProcessService;
    }

    @Scheduled(fixedDelayString = "${outbox.publish-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        OffsetDateTime now = OffsetDateTime.now();
        List<ProcessStartOutbox> due = outboxRepository.findByStatusIn(
                List.of(OutboxStatus.PENDING, OutboxStatus.RETRYABLE_FAILURE));
        for (ProcessStartOutbox event : due) {
            if (event.getNextAttemptAt() != null && event.getNextAttemptAt().isAfter(now)) {
                continue;
            }
            publish(event);
        }
    }

    private void publish(ProcessStartOutbox event) {
        try {
            Map<String, Object> variables = objectMapper.readValue(event.getPayloadJson(), new TypeReference<>() {
            });
            camundaProcessService.publishCreatePurchaseOrderMessage(
                    event.getId().toString(), event.getCorrelationKey(), variables);
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(OffsetDateTime.now());
            event.setLastError(null);
        } catch (Exception ex) {
            int attempts = event.getAttemptCount() + 1;
            event.setAttemptCount(attempts);
            event.setLastError(ex.getMessage());
            if (attempts >= MAX_ATTEMPTS) {
                event.setStatus(OutboxStatus.DEAD_LETTER);
                log.error("Outbox event {} moved to DEAD_LETTER after {} attempts", event.getId(), attempts, ex);
            } else {
                event.setStatus(OutboxStatus.RETRYABLE_FAILURE);
                event.setNextAttemptAt(OffsetDateTime.now().plusSeconds(Math.min(60L * attempts, 600L)));
                log.warn("Outbox event {} publish attempt {} failed: {}", event.getId(), attempts, ex.getMessage());
            }
        }
        event.setUpdatedAt(OffsetDateTime.now());
        outboxRepository.save(event);
    }
}
