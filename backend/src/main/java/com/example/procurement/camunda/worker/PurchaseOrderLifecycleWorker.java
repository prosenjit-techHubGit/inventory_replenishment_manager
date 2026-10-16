package com.example.procurement.camunda.worker;

import com.example.procurement.application.PurchaseOrderApplicationService;
import com.example.procurement.camunda.ProcessConstants;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Thin job workers for Purchase Order status transitions; all business logic lives in the application service. */
@Component
public class PurchaseOrderLifecycleWorker {

    private final PurchaseOrderApplicationService purchaseOrderApplicationService;

    public PurchaseOrderLifecycleWorker(PurchaseOrderApplicationService purchaseOrderApplicationService) {
        this.purchaseOrderApplicationService = purchaseOrderApplicationService;
    }

    /** First step after the CREATE_PURCHASE_ORDER message start event (plan section 46.4). */
    @JobWorker(type = ProcessConstants.JOB_REGISTER_PROCESS_START)
    public Map<String, Object> registerProcessStart(ActivatedJob job, @Variable(name = "purchaseOrderId") String purchaseOrderId) {
        boolean duplicate = purchaseOrderApplicationService.registerProcessStart(
                UUID.fromString(purchaseOrderId), String.valueOf(job.getProcessInstanceKey()));
        return Map.of("duplicateProcessStart", duplicate);
    }

    /** Loads the authoritative PO state as process variables once message-start correlation is settled. */
    @JobWorker(type = ProcessConstants.JOB_PREPARE_PO)
    public Map<String, Object> preparePo(@Variable(name = "purchaseOrderId") String purchaseOrderId) {
        return purchaseOrderApplicationService.preparePoVariables(UUID.fromString(purchaseOrderId));
    }

    @JobWorker(type = ProcessConstants.JOB_SUBMIT_FOR_APPROVAL)
    public void submitForApproval(@Variable(name = "purchaseOrderId") String purchaseOrderId) {
        purchaseOrderApplicationService.submitForApproval(UUID.fromString(purchaseOrderId));
    }

    @JobWorker(type = ProcessConstants.JOB_REJECT_PO)
    public void rejectPo(@Variable(name = "purchaseOrderId") String purchaseOrderId) {
        purchaseOrderApplicationService.reject(UUID.fromString(purchaseOrderId));
    }

    @JobWorker(type = ProcessConstants.JOB_APPLY_PO_REVISION)
    public void applyRevision(@Variable(name = "purchaseOrderId") String purchaseOrderId) {
        purchaseOrderApplicationService.applyRevision(UUID.fromString(purchaseOrderId));
    }

    @JobWorker(type = ProcessConstants.JOB_MARK_APPROVED)
    public void markApproved(@Variable(name = "purchaseOrderId") String purchaseOrderId) {
        purchaseOrderApplicationService.markApprovedAndAwaitingSupplierConfirmation(UUID.fromString(purchaseOrderId));
    }

    @JobWorker(type = ProcessConstants.JOB_MARK_SUPPLIER_REJECTED)
    public void markSupplierRejected(@Variable(name = "purchaseOrderId") String purchaseOrderId) {
        purchaseOrderApplicationService.markSupplierRejected(UUID.fromString(purchaseOrderId));
    }

    @JobWorker(type = ProcessConstants.JOB_RELEASE_PURCHASE_ORDER)
    public void release(@Variable(name = "purchaseOrderId") String purchaseOrderId) {
        purchaseOrderApplicationService.release(UUID.fromString(purchaseOrderId));
    }

    @JobWorker(type = ProcessConstants.JOB_COMPLETE_PO)
    public void completePo(@Variable(name = "purchaseOrderId") String purchaseOrderId) {
        purchaseOrderApplicationService.complete(UUID.fromString(purchaseOrderId));
    }
}
