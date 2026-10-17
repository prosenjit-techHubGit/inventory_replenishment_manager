package com.example.procurement.camunda.service;

import com.example.procurement.application.InventoryApplicationService;
import com.example.procurement.application.PurchaseOrderApplicationService;
import com.example.procurement.application.RecommendationApplicationService;
import com.example.procurement.application.ShipmentApplicationService;
import com.example.procurement.application.dto.TaskCompletionRequest;
import com.example.procurement.application.dto.TaskDetailResponse;
import com.example.procurement.application.dto.TaskSummaryResponse;
import com.example.procurement.camunda.ProcessConstants;
import com.example.procurement.domain.purchaseorder.ApprovalRole;
import com.example.procurement.domain.purchaseorder.PurchaseOrder;
import com.example.procurement.exception.BusinessValidationException;
import com.example.procurement.exception.NotFoundException;
import com.example.procurement.exception.ProcessIntegrationException;
import com.example.procurement.repository.PurchaseOrderRepository;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.client.api.search.response.Variable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Task façade over the Camunda user-task API (plan sections 18, 34.3, 42.2-42.4). Task
 * completion is authoritative for human workflow transitions; it never forwards an arbitrary
 * variables map from React directly to Camunda.
 */
@Service
public class TaskApplicationService {

    private final CamundaClient camundaClient;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderApplicationService purchaseOrderApplicationService;
    private final ShipmentApplicationService shipmentApplicationService;
    private final InventoryApplicationService inventoryApplicationService;
    private final RecommendationApplicationService recommendationApplicationService;

    public TaskApplicationService(
            CamundaClient camundaClient,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderApplicationService purchaseOrderApplicationService,
            ShipmentApplicationService shipmentApplicationService,
            InventoryApplicationService inventoryApplicationService,
            RecommendationApplicationService recommendationApplicationService) {
        this.camundaClient = camundaClient;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderApplicationService = purchaseOrderApplicationService;
        this.shipmentApplicationService = shipmentApplicationService;
        this.inventoryApplicationService = inventoryApplicationService;
        this.recommendationApplicationService = recommendationApplicationService;
    }

    public List<TaskSummaryResponse> search(String candidateGroup, boolean activeOnly) {
        try {
            List<UserTask> tasks = camundaClient.newUserTaskSearchRequest()
                    .filter(f -> {
                        if (candidateGroup != null && !candidateGroup.isBlank()) {
                            f.candidateGroup(candidateGroup);
                        }
                        if (activeOnly) {
                            f.state(UserTaskState.CREATED);
                        }
                    })
                    .execute()
                    .items();
            return tasks.stream().map(this::toSummary).toList();
        } catch (RuntimeException ex) {
            throw new ProcessIntegrationException("Failed to search tasks", ex);
        }
    }

    public TaskDetailResponse getTask(long taskKey) {
        UserTask task = fetchTask(taskKey);
        Map<String, Object> data = loadTaskVariables(task);
        if (ProcessConstants.ELEMENT_REVIEW_RECOMMENDATION.equals(task.getElementId())) {
            UUID recommendationId = recommendationApplicationService.resolveReviewRecommendationId(
                    data.get("itemId"), data.get("recommendationId"));
            data.put("recommendationId", recommendationId.toString());
        }
        return new TaskDetailResponse(
                String.valueOf(task.getUserTaskKey()),
                task.getElementId(),
                task.getName(),
                task.getBusinessId(),
                allowedActionsFor(task.getElementId()),
                data);
    }

    public void claim(long taskKey, String assignee) {
        try {
            camundaClient.newAssignUserTaskCommand(taskKey).assignee(assignee).allowOverride(true).execute();
        } catch (RuntimeException ex) {
            throw new ProcessIntegrationException("Failed to claim task " + taskKey, ex);
        }
    }

    public void complete(long taskKey, TaskCompletionRequest request, String actingUserId) {
        UserTask task = fetchTask(taskKey);
        Map<String, Object> variables = ProcessConstants.ELEMENT_REVIEW_RECOMMENDATION.equals(task.getElementId())
                ? dispatchRecommendationReview(task, request, actingUserId)
                : dispatchPurchaseOrderTask(task, request, actingUserId);
        try {
            camundaClient.newCompleteUserTaskCommand(taskKey)
                    .action(request.action())
                    .variables(variables)
                    .execute();
        } catch (RuntimeException ex) {
            throw new ProcessIntegrationException("Failed to complete task " + taskKey, ex);
        }
    }

    private Map<String, Object> dispatchPurchaseOrderTask(UserTask task, TaskCompletionRequest request, String actingUserId) {
        PurchaseOrder po = resolvePurchaseOrder(task);
        return dispatch(task.getElementId(), po.getId(), request, actingUserId);
    }

    /**
     * Message-start instances do not always carry a Camunda business key. Fall back to the
     * process variables published with CREATE_PURCHASE_ORDER ({@code poRequestId} / {@code purchaseOrderId}).
     */
    private PurchaseOrder resolvePurchaseOrder(UserTask task) {
        if (task.getBusinessId() != null && !task.getBusinessId().isBlank()) {
            var byBusinessId = purchaseOrderRepository.findByPoRequestId(task.getBusinessId());
            if (byBusinessId.isPresent()) {
                return byBusinessId.get();
            }
        }
        Map<String, Object> variables = loadTaskVariables(task);
        Object poRequestId = variables.get("poRequestId");
        if (poRequestId != null && !poRequestId.toString().isBlank()) {
            var byRequestId = purchaseOrderRepository.findByPoRequestId(stripJsonQuotes(poRequestId.toString()));
            if (byRequestId.isPresent()) {
                return byRequestId.get();
            }
        }
        Object purchaseOrderId = variables.get("purchaseOrderId");
        if (purchaseOrderId != null && !purchaseOrderId.toString().isBlank()) {
            UUID id = UUID.fromString(stripJsonQuotes(purchaseOrderId.toString()));
            return purchaseOrderRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No purchase order found for id " + id));
        }
        throw new NotFoundException("No purchase order found for task " + task.getUserTaskKey());
    }

    private static String stripJsonQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    /** Review Recommendation tasks run inside a parallel multi-instance body, so the batch's shared
     * businessId cannot identify them; the recommendation id is carried as a task-local variable instead. */
    private Map<String, Object> dispatchRecommendationReview(
            UserTask task, TaskCompletionRequest request, String actingUserId) {
        Map<String, Object> taskVariables = loadTaskVariables(task);
        UUID recommendationId = recommendationApplicationService.resolveReviewRecommendationId(
                taskVariables.get("itemId"), taskVariables.get("recommendationId"));
        return recommendationApplicationService.recordReviewDecision(
                recommendationId, request.action(), request.variables(), actingUserId);
    }

    private Map<String, Object> loadTaskVariables(UserTask task) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Long> scopes = new HashMap<>();
        try {
            List<Variable> variables =
                    camundaClient.newUserTaskVariableSearchRequest(task.getUserTaskKey()).execute().items();
            for (Variable variable : variables) {
                String name = variable.getName();
                Long scope = variable.getScopeKey();
                if (!data.containsKey(name)
                        || isPreferredScope(
                                scope, scopes.get(name), task.getElementInstanceKey(), task.getProcessInstanceKey())) {
                    data.put(name, coerceVariable(variable));
                    scopes.put(name, scope);
                }
            }
        } catch (RuntimeException ex) {
            throw new ProcessIntegrationException("Failed to load task variables for " + task.getUserTaskKey(), ex);
        }
        return data;
    }

    private static boolean isPreferredScope(
            Long candidate, Long current, Long elementInstanceKey, Long processInstanceKey) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        if (candidate.equals(elementInstanceKey) && !current.equals(elementInstanceKey)) {
            return true;
        }
        if (current.equals(elementInstanceKey)) {
            return false;
        }
        boolean candidateLocal = processInstanceKey == null || !candidate.equals(processInstanceKey);
        boolean currentLocal = processInstanceKey == null || !current.equals(processInstanceKey);
        if (candidateLocal != currentLocal) {
            return candidateLocal;
        }
        return candidate > current;
    }

    private static Object coerceVariable(Variable variable) {
        Object typed = variable.getValueAsType(Object.class);
        if (typed instanceof String text) {
            return stripJsonQuotes(text);
        }
        return typed != null ? typed : stripJsonQuotes(variable.getValue());
    }

    private Map<String, Object> dispatch(
            String elementId, UUID purchaseOrderId, TaskCompletionRequest request, String actingUserId) {
        String comment = stringVariable(request, "comment");
        return switch (elementId) {
            case ProcessConstants.ELEMENT_PROCUREMENT_APPROVAL -> approvalVariables(
                    purchaseOrderId, ApprovalRole.PROCUREMENT_MANAGER, request, actingUserId, comment);
            case ProcessConstants.ELEMENT_FINANCE_APPROVAL -> approvalVariables(
                    purchaseOrderId, ApprovalRole.FINANCE_APPROVER, request, actingUserId, comment);
            case ProcessConstants.ELEMENT_SUPPLY_CHAIN_APPROVAL -> approvalVariables(
                    purchaseOrderId, ApprovalRole.SUPPLY_CHAIN_MANAGER, request, actingUserId, comment);
            case ProcessConstants.ELEMENT_REVISE_PO -> {
                purchaseOrderApplicationService.applyRevisionEdits(purchaseOrderId, request.variables());
                yield Map.of();
            }
            case ProcessConstants.ELEMENT_SUPPLIER_CONFIRMATION -> {
                String revisedDate = stringVariable(request, "revisedDeliveryDate");
                String confirmationStatus = purchaseOrderApplicationService.recordSupplierConfirmation(
                        purchaseOrderId, request.action(), revisedDate);
                Map<String, Object> vars = new HashMap<>();
                vars.put("confirmationStatus", confirmationStatus);
                if (revisedDate != null) {
                    vars.put("requiredDeliveryDate", revisedDate);
                }
                yield vars;
            }
            case ProcessConstants.ELEMENT_TRACK_SHIPMENT -> {
                String trackingReference = stringVariable(request, "trackingReference");
                String actualDeliveryDate = stringVariable(request, "actualDeliveryDate");
                shipmentApplicationService.recordDelivery(
                        purchaseOrderId,
                        actualDeliveryDate != null ? LocalDate.parse(actualDeliveryDate) : LocalDate.now(),
                        trackingReference);
                yield Map.of("shipmentStatus", "DELIVERED");
            }
            case ProcessConstants.ELEMENT_GOODS_RECEIPT -> {
                UUID goodsReceiptId = inventoryApplicationService.recordGoodsReceipt(
                        purchaseOrderId, request.variables());
                yield Map.of("goodsReceiptId", goodsReceiptId.toString());
            }
            default -> throw new BusinessValidationException("No completion handler for task element " + elementId);
        };
    }

    private Map<String, Object> approvalVariables(
            UUID purchaseOrderId, ApprovalRole role, TaskCompletionRequest request, String actingUserId, String comment) {
        String outcome = purchaseOrderApplicationService.recordApprovalDecision(
                purchaseOrderId, role, request.action(), comment, actingUserId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("approvalOutcome", outcome);
        return vars;
    }

    private String stringVariable(TaskCompletionRequest request, String name) {
        Object value = request.variables().get(name);
        return value == null ? null : value.toString();
    }

    private List<String> allowedActionsFor(String elementId) {
        return switch (elementId) {
            case ProcessConstants.ELEMENT_PROCUREMENT_APPROVAL,
                    ProcessConstants.ELEMENT_FINANCE_APPROVAL,
                    ProcessConstants.ELEMENT_SUPPLY_CHAIN_APPROVAL ->
                    List.of("APPROVE", "RETURN_FOR_REVISION", "REJECT");
            case ProcessConstants.ELEMENT_REVISE_PO -> List.of("SUBMIT_REVISION");
            case ProcessConstants.ELEMENT_SUPPLIER_CONFIRMATION ->
                    List.of("CONFIRMED", "REVISED_DATE", "REJECTED");
            case ProcessConstants.ELEMENT_TRACK_SHIPMENT -> List.of("DELIVERED");
            case ProcessConstants.ELEMENT_GOODS_RECEIPT -> List.of("RECORD_RECEIPT");
            case ProcessConstants.ELEMENT_REVIEW_RECOMMENDATION -> List.of("ACCEPT", "MODIFY_AND_ACCEPT", "REJECT");
            default -> List.of();
        };
    }

    private UserTask fetchTask(long taskKey) {
        try {
            return camundaClient.newUserTaskGetRequest(taskKey).execute();
        } catch (RuntimeException ex) {
            throw new NotFoundException("Task not found: " + taskKey);
        }
    }

    private TaskSummaryResponse toSummary(UserTask task) {
        return new TaskSummaryResponse(
                String.valueOf(task.getUserTaskKey()),
                task.getName(),
                task.getElementId(),
                task.getBpmnProcessId(),
                task.getAssignee(),
                task.getBusinessId(),
                task.getCreationDate());
    }
}
