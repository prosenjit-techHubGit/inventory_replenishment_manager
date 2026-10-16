package com.example.procurement.application;

import com.example.procurement.application.dto.ApprovalDto;
import com.example.procurement.application.dto.CreatePurchaseOrderRequest;
import com.example.procurement.application.dto.GoodsReceiptDto;
import com.example.procurement.application.dto.PurchaseOrderDetailResponse;
import com.example.procurement.application.dto.PurchaseOrderResponse;
import com.example.procurement.application.dto.ShipmentDto;
import com.example.procurement.camunda.mapper.ProcessVariableMapper;
import com.example.procurement.domain.RiskLevel;
import com.example.procurement.domain.Urgency;
import com.example.procurement.domain.idempotency.IdempotencyRequest;
import com.example.procurement.domain.idempotency.IdempotencyStatus;
import com.example.procurement.domain.inventory.InventoryItem;
import com.example.procurement.domain.purchaseorder.ApprovalOutcome;
import com.example.procurement.domain.purchaseorder.ApprovalRole;
import com.example.procurement.domain.purchaseorder.InitiationType;
import com.example.procurement.domain.purchaseorder.PurchaseOrder;
import com.example.procurement.domain.purchaseorder.PurchaseOrderApproval;
import com.example.procurement.domain.purchaseorder.PurchaseOrderStatus;
import com.example.procurement.domain.shipment.Shipment;
import com.example.procurement.domain.shipment.ShipmentStatus;
import com.example.procurement.domain.supplier.Supplier;
import com.example.procurement.exception.BusinessValidationException;
import com.example.procurement.exception.IdempotencyConflictException;
import com.example.procurement.exception.NotFoundException;
import com.example.procurement.repository.GoodsReceiptRepository;
import com.example.procurement.repository.IdempotencyRequestRepository;
import com.example.procurement.repository.InventoryItemRepository;
import com.example.procurement.repository.PurchaseOrderApprovalRepository;
import com.example.procurement.repository.PurchaseOrderRepository;
import com.example.procurement.repository.ShipmentRepository;
import com.example.procurement.repository.SupplierRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Owns the Purchase Order lifecycle: idempotent manual creation and the status
 * transitions driven by Camunda job workers / task completions (plan sections 8, 35-38).
 */
@Service
public class PurchaseOrderApplicationService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderApprovalRepository approvalRepository;
    private final IdempotencyRequestRepository idempotencyRequestRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SupplierRepository supplierRepository;
    private final ShipmentRepository shipmentRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final ProcessStartOutboxService processStartOutboxService;
    private final ProcessVariableMapper processVariableMapper;
    // Jackson 3 JsonMapper includes Java Time; the classic Jackson 2 ObjectMapper cannot serialize LocalDate.
    private final JsonMapper jsonMapper = new JsonMapper();

    public PurchaseOrderApplicationService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderApprovalRepository approvalRepository,
            IdempotencyRequestRepository idempotencyRequestRepository,
            InventoryItemRepository inventoryItemRepository,
            SupplierRepository supplierRepository,
            ShipmentRepository shipmentRepository,
            GoodsReceiptRepository goodsReceiptRepository,
            ProcessStartOutboxService processStartOutboxService,
            ProcessVariableMapper processVariableMapper) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.approvalRepository = approvalRepository;
        this.idempotencyRequestRepository = idempotencyRequestRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.supplierRepository = supplierRepository;
        this.shipmentRepository = shipmentRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.processStartOutboxService = processStartOutboxService;
        this.processVariableMapper = processVariableMapper;
    }

    /**
     * Idempotent manual PO creation (plan sections 38.1, 45.1, 45.2). Creates the PO plus its
     * {@code process_start_outbox} row in one local transaction; a background publisher starts
     * Process 2 asynchronously, so no Camunda call happens on this request path.
     */
    @Transactional
    public PurchaseOrderResponse createManualPurchaseOrder(UUID idempotencyKey, CreatePurchaseOrderRequest request) {
        String requestHash = hash(request);
        IdempotencyRequest idempotencyRequest = idempotencyRequestRepository.findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (idempotencyRequest != null) {
            if (!idempotencyRequest.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency-Key " + idempotencyKey + " was already used with a different request body");
            }
            if (idempotencyRequest.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new BusinessValidationException(
                        "A request with this Idempotency-Key is already being processed");
            }
            return readResponse(idempotencyRequest.getResponseJson());
        }

        UUID itemId = parseUuid(request.itemId(), "itemId");
        UUID supplierId = parseUuid(request.supplierId(), "supplierId");
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessValidationException("Unknown itemId " + request.itemId()));
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessValidationException("Unknown supplierId " + request.supplierId()));
        if (!supplier.isActive()) {
            throw new BusinessValidationException("Supplier " + supplier.getCode() + " is not active");
        }

        OffsetDateTime now = OffsetDateTime.now();
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(generatePoNumber());
        po.setPoRevision(1);
        po.setInitiationType(InitiationType.MANUAL);
        po.setItemId(itemId);
        po.setSupplierId(supplierId);
        po.setQuantity(request.quantity());
        po.setUnitPrice(request.unitPrice());
        po.setTotalValue(request.quantity().multiply(request.unitPrice()));
        po.setRequiredDeliveryDate(request.requiredDeliveryDate());
        po.setUrgency(request.urgency());
        po.setSupplierRisk(supplier.getRiskLevel() == null ? RiskLevel.LOW : supplier.getRiskLevel());
        po.setBusinessJustification(request.businessJustification());
        po.setStatus(PurchaseOrderStatus.PENDING_PROCESS_START);
        po.setPoRequestId("MANUAL:" + idempotencyKey);
        po.setCreatedAt(now);
        po.setUpdatedAt(now);
        po = purchaseOrderRepository.save(po);
        processStartOutboxService.enqueueCreatePurchaseOrderStart(po);

        PurchaseOrderResponse response = toResponse(po);
        IdempotencyRequest newIdempotencyRequest = new IdempotencyRequest();
        newIdempotencyRequest.setIdempotencyKey(idempotencyKey);
        newIdempotencyRequest.setOperation("CREATE_PURCHASE_ORDER");
        newIdempotencyRequest.setRequestHash(requestHash);
        newIdempotencyRequest.setStatus(IdempotencyStatus.COMPLETED);
        newIdempotencyRequest.setResourceId(po.getId());
        newIdempotencyRequest.setResponseJson(writeResponse(response));
        newIdempotencyRequest.setCreatedAt(now);
        newIdempotencyRequest.setUpdatedAt(now);
        idempotencyRequestRepository.save(newIdempotencyRequest);
        return response;
    }

    /**
     * Job worker `register-process-start` (plan section 46.4). Returns {@code true} when this
     * process instance is a genuine duplicate start that must not proceed with business logic.
     */
    @Transactional
    public boolean registerProcessStart(UUID purchaseOrderId, String processInstanceKey) {
        int updated = purchaseOrderRepository.registerProcessInstanceKeyIfAbsent(purchaseOrderId, processInstanceKey);
        if (updated == 1) {
            return false;
        }
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        return !processInstanceKey.equals(po.getCamundaProcessInstanceKey());
    }

    /** Job worker `prepare-po`: loads the authoritative PO state as process variables (plan section 45.4). */
    @Transactional(readOnly = true)
    public Map<String, Object> preparePoVariables(UUID purchaseOrderId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        InventoryItem item = inventoryItemRepository.findById(po.getItemId())
                .orElseThrow(() -> new NotFoundException("Inventory item not found: " + po.getItemId()));
        return processVariableMapper.toCreatePurchaseOrderVariables(po, item.isCriticalItem());
    }

    /** Shared PO numbering sequence, also used by the automated recommendation-acceptance handoff. */
    public String generatePoNumber() {
        long seq = purchaseOrderRepository.nextPoNumberSequenceValue();
        return "PO-" + Year.now() + "-" + String.format("%06d", seq);
    }

    /** Job worker `submit-po-for-approval`: IN_PREPARATION -> PENDING_APPROVAL. */
    @Transactional
    public void submitForApproval(UUID purchaseOrderId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        po.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);
    }

    /**
     * Persists the approval decision for one role and, for REJECTED/RETURN_FOR_REVISION,
     * updates the PO status immediately (plan section 35.4/35.5). Returns the Camunda
     * variable value ({@code approvalOutcome}) to carry the decision back into the process.
     */
    @Transactional
    public String recordApprovalDecision(
            UUID purchaseOrderId, ApprovalRole role, String action, String comment, String approverUserId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        ApprovalOutcome outcome = switch (action) {
            case "APPROVE" -> ApprovalOutcome.APPROVED;
            case "RETURN_FOR_REVISION" -> ApprovalOutcome.RETURNED_FOR_REVISION;
            case "REJECT" -> ApprovalOutcome.REJECTED;
            default -> throw new BusinessValidationException("Unsupported approval action " + action);
        };

        PurchaseOrderApproval approval = approvalRepository
                .findByPurchaseOrderIdAndPoRevisionAndApprovalRole(po.getId(), po.getPoRevision(), role)
                .orElseGet(PurchaseOrderApproval::new);
        approval.setPurchaseOrderId(po.getId());
        approval.setPoRevision(po.getPoRevision());
        approval.setApprovalRole(role);
        approval.setApproverUserId(approverUserId);
        approval.setOutcome(outcome);
        approval.setComment(comment);
        if (approval.getCreatedAt() == null) {
            approval.setCreatedAt(OffsetDateTime.now());
        }
        approval.setCompletedAt(OffsetDateTime.now());
        approvalRepository.save(approval);

        if (outcome == ApprovalOutcome.REJECTED) {
            po.setStatus(PurchaseOrderStatus.REJECTED);
            po.setUpdatedAt(OffsetDateTime.now());
            purchaseOrderRepository.save(po);
        } else if (outcome == ApprovalOutcome.RETURNED_FOR_REVISION) {
            po.setStatus(PurchaseOrderStatus.REVISION_REQUIRED);
            po.setUpdatedAt(OffsetDateTime.now());
            purchaseOrderRepository.save(po);
        }
        return outcome.name();
    }

    /** UserTask_RevisePO completion: the Procurement Buyer edits the PO fields (plan section 35.4). */
    @Transactional
    public void applyRevisionEdits(UUID purchaseOrderId, Map<String, Object> edits) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        if (edits.containsKey("quantity")) {
            po.setQuantity(new BigDecimal(edits.get("quantity").toString()));
        }
        if (edits.containsKey("unitPrice")) {
            po.setUnitPrice(new BigDecimal(edits.get("unitPrice").toString()));
        }
        if (edits.containsKey("requiredDeliveryDate")) {
            po.setRequiredDeliveryDate(java.time.LocalDate.parse(edits.get("requiredDeliveryDate").toString()));
        }
        if (edits.containsKey("supplierId")) {
            UUID newSupplierId = parseUuid(edits.get("supplierId").toString(), "supplierId");
            Supplier supplier = supplierRepository.findById(newSupplierId)
                    .orElseThrow(() -> new BusinessValidationException("Unknown supplierId"));
            po.setSupplierId(newSupplierId);
            po.setSupplierRisk(supplier.getRiskLevel());
        }
        if (edits.containsKey("businessJustification")) {
            po.setBusinessJustification(edits.get("businessJustification").toString());
        }
        if (edits.containsKey("urgency")) {
            po.setUrgency(Urgency.valueOf(edits.get("urgency").toString()));
        }
        po.setTotalValue(po.getQuantity().multiply(po.getUnitPrice()));
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);
    }

    /** Job worker `apply-po-revision`: increments poRevision and restarts the approval chain. */
    @Transactional
    public void applyRevision(UUID purchaseOrderId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        po.setPoRevision(po.getPoRevision() + 1);
        po.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);
    }

    /** Job worker `reject-po`. */
    @Transactional
    public void reject(UUID purchaseOrderId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        po.setStatus(PurchaseOrderStatus.REJECTED);
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);
    }

    /** Job worker `mark-po-approved`: all required approvals are APPROVED; moves straight into supplier confirmation. */
    @Transactional
    public void markApprovedAndAwaitingSupplierConfirmation(UUID purchaseOrderId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        po.setStatus(PurchaseOrderStatus.PENDING_SUPPLIER_CONFIRMATION);
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);
    }

    /** UserTask_SupplierConfirmation completion (plan section 12). */
    @Transactional
    public String recordSupplierConfirmation(
            UUID purchaseOrderId, String confirmationStatus, String revisedDeliveryDate) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        switch (confirmationStatus) {
            case "CONFIRMED" -> po.setStatus(PurchaseOrderStatus.SUPPLIER_CONFIRMED);
            case "REJECTED" -> po.setStatus(PurchaseOrderStatus.SUPPLIER_REJECTED);
            case "REVISED_DATE" -> {
                po.setStatus(PurchaseOrderStatus.SUPPLIER_DATE_REVISED);
                if (revisedDeliveryDate != null && !revisedDeliveryDate.isBlank()) {
                    po.setRequiredDeliveryDate(java.time.LocalDate.parse(revisedDeliveryDate));
                }
            }
            default -> throw new BusinessValidationException("Unsupported confirmation status " + confirmationStatus);
        }
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);
        return confirmationStatus;
    }

    /** Job worker `mark-supplier-rejected`. */
    @Transactional
    public void markSupplierRejected(UUID purchaseOrderId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        po.setStatus(PurchaseOrderStatus.SUPPLIER_REJECTED);
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);
    }

    /** Job worker `release-purchase-order`: RELEASED + plans the first shipment. */
    @Transactional
    public void release(UUID purchaseOrderId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        po.setStatus(PurchaseOrderStatus.RELEASED);
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);

        Shipment shipment = new Shipment();
        shipment.setPurchaseOrderId(po.getId());
        shipment.setStatus(ShipmentStatus.PLANNED);
        shipment.setPlannedDeliveryDate(po.getRequiredDeliveryDate());
        shipment.setSupplierShipmentReference(po.getPoNumber() + "-S1");
        shipment.setUpdatedAt(OffsetDateTime.now());
        shipmentRepository.save(shipment);

        po.setStatus(PurchaseOrderStatus.IN_FULFILLMENT);
        purchaseOrderRepository.save(po);
    }

    /** Job worker `complete-po`: terminal success state once fully received. */
    @Transactional
    public void complete(UUID purchaseOrderId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        po.setStatus(PurchaseOrderStatus.COMPLETED);
        po.setUpdatedAt(OffsetDateTime.now());
        purchaseOrderRepository.save(po);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDetailResponse getById(UUID purchaseOrderId) {
        PurchaseOrder po = getOrThrow(purchaseOrderId);
        InventoryItem item = inventoryItemRepository.findById(po.getItemId()).orElse(null);
        Supplier supplier = supplierRepository.findById(po.getSupplierId()).orElse(null);
        var receipts = goodsReceiptRepository.findByPurchaseOrderIdOrderByReceiptNumber(po.getId());
        BigDecimal cumulativeAccepted = receipts.stream()
                .map(receipt -> receipt.getAcceptedQuantity() == null ? BigDecimal.ZERO : receipt.getAcceptedQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = po.getQuantity().subtract(cumulativeAccepted).max(BigDecimal.ZERO);
        return new PurchaseOrderDetailResponse(
                toResponse(po),
                item != null ? item.getSku() : null,
                item != null ? item.getName() : null,
                item != null && item.isCriticalItem(),
                supplier != null ? supplier.getCode() : null,
                supplier != null ? supplier.getName() : null,
                shipmentRepository.findByPurchaseOrderId(po.getId()).stream()
                        .map(s -> new ShipmentDto(
                                s.getId(),
                                s.getStatus(),
                                s.getPlannedDeliveryDate(),
                                s.getActualDeliveryDate(),
                                s.getSupplierShipmentReference(),
                                s.getTrackingReference()))
                        .toList(),
                receipts.stream()
                        .map(r -> new GoodsReceiptDto(
                                r.getId(),
                                r.getReceiptNumber(),
                                r.getReceivedQuantity(),
                                r.getAcceptedQuantity(),
                                r.getRejectedQuantity(),
                                r.getReceiptDate(),
                                r.getNotes()))
                        .toList(),
                approvalRepository.findByPurchaseOrderIdOrderByPoRevisionAscCreatedAtAsc(po.getId()).stream()
                        .map(a -> new ApprovalDto(
                                a.getId(),
                                a.getPoRevision(),
                                a.getApprovalRole(),
                                a.getOutcome(),
                                a.getApproverUserId(),
                                a.getComment(),
                                a.getCompletedAt() == null ? null : a.getCompletedAt().toString()))
                        .toList(),
                cumulativeAccepted,
                remaining);
    }

    @Transactional(readOnly = true)
    public java.util.List<PurchaseOrderResponse> findAll() {
        return purchaseOrderRepository.findAll().stream().map(this::toResponse).toList();
    }

    private PurchaseOrder getOrThrow(UUID purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found: " + purchaseOrderId));
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Invalid " + fieldName + ": " + value);
        }
    }

    private String hash(CreatePurchaseOrderRequest request) {
        try {
            String canonical = jsonMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException | JacksonException ex) {
            throw new BusinessValidationException("Unable to hash request body");
        }
    }

    private String writeResponse(PurchaseOrderResponse response) {
        try {
            return jsonMapper.writeValueAsString(response);
        } catch (JacksonException ex) {
            throw new BusinessValidationException("Unable to serialize response");
        }
    }

    private PurchaseOrderResponse readResponse(String json) {
        try {
            return jsonMapper.readValue(json, PurchaseOrderResponse.class);
        } catch (Exception ex) {
            throw new BusinessValidationException("Unable to deserialize stored idempotent response");
        }
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        return new PurchaseOrderResponse(
                po.getId(),
                po.getPoNumber(),
                po.getPoRevision(),
                po.getStatus(),
                po.getInitiationType(),
                po.getCamundaProcessInstanceKey(),
                po.getPoRequestId(),
                po.getItemId(),
                po.getSupplierId(),
                po.getRecommendationId(),
                po.getQuantity(),
                po.getUnitPrice(),
                po.getTotalValue(),
                po.getRequiredDeliveryDate(),
                po.getUrgency(),
                po.getSupplierRisk(),
                po.getBusinessJustification(),
                po.getCreatedAt() == null ? null : po.getCreatedAt().toString());
    }
}
