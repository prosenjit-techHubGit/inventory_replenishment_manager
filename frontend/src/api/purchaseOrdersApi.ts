import type { CreatePurchaseOrderRequest, PurchaseOrder, PurchaseOrderDetail } from "../models/types";
import { request } from "./httpClient";

export function listPurchaseOrders(): Promise<PurchaseOrder[]> {
  return request("/api/v1/purchase-orders");
}

export function getPurchaseOrder(id: string): Promise<PurchaseOrderDetail> {
  return request(`/api/v1/purchase-orders/${id}`);
}

export function createPurchaseOrder(
  body: CreatePurchaseOrderRequest,
  idempotencyKey: string,
): Promise<PurchaseOrder> {
  return request("/api/v1/purchase-orders", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey },
    body: JSON.stringify(body),
  });
}
