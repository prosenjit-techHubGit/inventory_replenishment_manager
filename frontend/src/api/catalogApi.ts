import type { InventoryItem, Supplier } from "../models/types";
import { request } from "./httpClient";

export function listInventory(): Promise<InventoryItem[]> {
  return request("/api/v1/inventory");
}

export function listSuppliers(): Promise<Supplier[]> {
  return request("/api/v1/suppliers");
}
