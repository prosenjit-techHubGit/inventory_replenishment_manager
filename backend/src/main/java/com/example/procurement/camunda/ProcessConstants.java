package com.example.procurement.camunda;

/** Centralizes BPMN process/element ids, job types and candidate groups (plan section 29). */
public final class ProcessConstants {

    public static final String CREATE_PURCHASE_ORDER_PROCESS_ID = "create-purchase-order";
    public static final String REPLENISHMENT_RECOMMENDATION_PROCESS_ID = "replenishment-recommendation";

    /** Message start event correlation (plan section 44.2); poRequestId is the correlation key. */
    public static final String MESSAGE_CREATE_PURCHASE_ORDER = "CREATE_PURCHASE_ORDER";

    // User task element ids
    public static final String ELEMENT_PROCUREMENT_APPROVAL = "UserTask_ProcurementApproval";
    public static final String ELEMENT_FINANCE_APPROVAL = "UserTask_FinanceApproval";
    public static final String ELEMENT_SUPPLY_CHAIN_APPROVAL = "UserTask_SupplyChainApproval";
    public static final String ELEMENT_REVISE_PO = "UserTask_RevisePO";
    public static final String ELEMENT_SUPPLIER_CONFIRMATION = "UserTask_SupplierConfirmation";
    public static final String ELEMENT_TRACK_SHIPMENT = "UserTask_TrackShipment";
    public static final String ELEMENT_GOODS_RECEIPT = "UserTask_GoodsReceipt";
    public static final String ELEMENT_REVIEW_RECOMMENDATION = "UserTask_ReviewRecommendation";

    // Job worker types
    public static final String JOB_REGISTER_PROCESS_START = "register-process-start";
    public static final String JOB_PREPARE_PO = "prepare-po";
    public static final String JOB_SUBMIT_FOR_APPROVAL = "submit-po-for-approval";
    public static final String JOB_REJECT_PO = "reject-po";
    public static final String JOB_APPLY_PO_REVISION = "apply-po-revision";
    public static final String JOB_MARK_APPROVED = "mark-po-approved";
    public static final String JOB_MARK_SUPPLIER_REJECTED = "mark-supplier-rejected";
    public static final String JOB_RELEASE_PURCHASE_ORDER = "release-purchase-order";
    public static final String JOB_FLAG_SHIPMENT_DELAYED = "flag-shipment-delayed";
    public static final String JOB_UPDATE_INVENTORY = "update-inventory";
    public static final String JOB_COMPLETE_PO = "complete-po";

    // Replenishment recommendation job worker types (plan section 7)
    public static final String JOB_LIST_REPLENISHMENT_CANDIDATES = "list-replenishment-candidates";
    public static final String JOB_CHECK_REPLENISHMENT = "check-replenishment";
    public static final String JOB_CREATE_RECOMMENDATION = "create-recommendation";
    public static final String JOB_PREPARE_PO_HANDOFF = "prepare-po-handoff";

    // Candidate groups (plan section 25)
    public static final String GROUP_PROCUREMENT_MANAGER = "PROCUREMENT_MANAGER";
    public static final String GROUP_PROCUREMENT_BUYER = "PROCUREMENT_BUYER";
    public static final String GROUP_FINANCE_APPROVER = "FINANCE_APPROVER";
    public static final String GROUP_SUPPLY_CHAIN_MANAGER = "SUPPLY_CHAIN_MANAGER";
    public static final String GROUP_SUPPLIER_USER = "SUPPLIER_USER";
    public static final String GROUP_WAREHOUSE_RECEIVER = "WAREHOUSE_RECEIVER";
    public static final String GROUP_INVENTORY_PLANNER = "INVENTORY_PLANNER";

    private ProcessConstants() {
    }
}
