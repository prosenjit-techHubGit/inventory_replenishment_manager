package com.example.procurement.application;

import com.example.procurement.camunda.ProcessConstants;
import com.example.procurement.domain.outbox.OutboxStatus;
import com.example.procurement.domain.outbox.ProcessStartOutbox;
import com.example.procurement.domain.purchaseorder.PurchaseOrder;
import com.example.procurement.exception.BusinessValidationException;
import com.example.procurement.repository.ProcessStartOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared transactional-outbox enqueue logic for both PO creation paths (plan sections 44-46).
 * Writing the outbox row is always part of the same local transaction as the PurchaseOrder write;
 * a background {@link com.example.procurement.camunda.service.OutboxPublisher} does the actual
 * Camunda message publication.
 */
@Service
public class ProcessStartOutboxService {

    private final ProcessStartOutboxRepository outboxRepository;
    // Spring Boot 4 autoconfigures a Jackson 3 JsonMapper, not this classic ObjectMapper; construct our own.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProcessStartOutboxService(ProcessStartOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /** Idempotent: a retry that finds an existing row for the same {@code poRequestId} is a no-op. */
    @Transactional
    public void enqueueCreatePurchaseOrderStart(PurchaseOrder po) {
        if (outboxRepository.findByPoRequestId(po.getPoRequestId()).isPresent()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("purchaseOrderId", po.getId().toString());
        payload.put("poNumber", po.getPoNumber());
        payload.put("poRequestId", po.getPoRequestId());
        payload.put("initiationType", po.getInitiationType().name());
        payload.put("recommendationId", po.getRecommendationId() == null ? null : po.getRecommendationId().toString());

        OffsetDateTime now = OffsetDateTime.now();
        ProcessStartOutbox outbox = new ProcessStartOutbox();
        outbox.setId(UUID.randomUUID());
        outbox.setPoRequestId(po.getPoRequestId());
        outbox.setPurchaseOrderId(po.getId());
        outbox.setMessageName(ProcessConstants.MESSAGE_CREATE_PURCHASE_ORDER);
        outbox.setCorrelationKey(po.getPoRequestId());
        outbox.setPayloadJson(writeJson(payload));
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setAttemptCount(0);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);
        outboxRepository.save(outbox);
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessValidationException("Unable to serialize outbox payload");
        }
    }
}
