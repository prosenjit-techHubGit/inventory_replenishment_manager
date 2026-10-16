export type PurchaseOrderStatus =
  | "DRAFT"
  | "PENDING_PROCESS_START"
  | "IN_PREPARATION"
  | "PENDING_APPROVAL"
  | "REVISION_REQUIRED"
  | "REJECTED"
  | "APPROVED"
  | "PENDING_SUPPLIER_CONFIRMATION"
  | "SUPPLIER_REJECTED"
  | "SUPPLIER_DATE_REVISED"
  | "SUPPLIER_CONFIRMED"
  | "RELEASED"
  | "IN_FULFILLMENT"
  | "PARTIALLY_RECEIVED"
  | "RECEIVED"
  | "COMPLETED"
  | "CANCELLED"
  | "START_FAILED";

export type InitiationType = "MANUAL" | "REPLENISHMENT";
export type Urgency = "NORMAL" | "HIGH" | "URGENT";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type RecommendationStatus =
  | "PENDING_REVIEW"
  | "ACCEPTED"
  | "REJECTED"
  | "SUPERSEDED"
  | "FAILED_MANUAL_REVIEW";

export type TaskElementId =
  | "UserTask_ProcurementApproval"
  | "UserTask_FinanceApproval"
  | "UserTask_SupplyChainApproval"
  | "UserTask_RevisePO"
  | "UserTask_SupplierConfirmation"
  | "UserTask_TrackShipment"
  | "UserTask_GoodsReceipt"
  | "UserTask_ReviewRecommendation"
  | string;

export interface PurchaseOrder {
  purchaseOrderId: string;
  poNumber: string;
  poRevision: number;
  status: PurchaseOrderStatus;
  initiationType: InitiationType;
  processInstanceKey: string | null;
  poRequestId: string;
  itemId: string;
  supplierId: string;
  recommendationId: string | null;
  quantity: number;
  unitPrice: number;
  totalValue: number;
  requiredDeliveryDate: string;
  urgency: Urgency;
  supplierRisk: RiskLevel;
  businessJustification: string | null;
  createdAt: string;
}

export interface Shipment {
  id: string;
  status: string;
  plannedDeliveryDate: string | null;
  actualDeliveryDate: string | null;
  supplierShipmentReference: string | null;
  trackingReference: string | null;
}

export interface GoodsReceipt {
  id: string;
  receiptNumber: number;
  receivedQuantity: number;
  acceptedQuantity: number;
  rejectedQuantity: number;
  receiptDate: string;
  notes: string | null;
}

export interface Approval {
  id: string;
  poRevision: number;
  approvalRole: string;
  outcome: string | null;
  approverUserId: string | null;
  comment: string | null;
  completedAt: string | null;
}

export interface PurchaseOrderDetail {
  purchaseOrder: PurchaseOrder;
  itemSku: string | null;
  itemName: string | null;
  criticalItem: boolean;
  supplierCode: string | null;
  supplierName: string | null;
  shipments: Shipment[];
  receipts: GoodsReceipt[];
  approvals: Approval[];
  cumulativeAcceptedQuantity: number;
  remainingQuantity: number;
}

export interface InventoryItem {
  id: string;
  sku: string;
  name: string;
  onHandQuantity: number;
  reorderLevel: number;
  openPurchaseQuantity: number;
  projectedAvailable: number;
  criticalItem: boolean;
  replenishmentRequired: boolean;
}

export interface Supplier {
  id: string;
  code: string;
  name: string;
  active: boolean;
  unitPrice: number;
  leadTimeDays: number;
  deliveryScore: number | null;
  qualityScore: number | null;
  riskLevel: RiskLevel;
}

export interface Recommendation {
  recommendationId: string;
  itemId: string;
  currentStock: number;
  recommendedQuantity: number;
  supplierId: string;
  targetDeliveryDate: string;
  riskLevel: RiskLevel;
  rationale: string;
  status: RecommendationStatus;
}

export interface TaskSummary {
  taskKey: string;
  name: string;
  elementId: TaskElementId;
  bpmnProcessId: string;
  assignee: string | null;
  businessId: string | null;
  creationDate: string;
}

export interface TaskDetail {
  taskKey: string;
  taskType: TaskElementId;
  name: string;
  businessId: string | null;
  allowedActions: string[];
  data: Record<string, unknown>;
}

export interface ApiError {
  code: string;
  message: string;
}

export interface CreatePurchaseOrderRequest {
  itemId: string;
  quantity: number;
  supplierId: string;
  unitPrice: number;
  requiredDeliveryDate: string;
  urgency: Urgency;
  businessJustification: string;
}
