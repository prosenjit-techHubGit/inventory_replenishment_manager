import type { PurchaseOrderStatus } from "../models/types";

export function money(value: number | string | null | undefined): string {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(amount);
}

export function qty(value: number | string | null | undefined): string {
  return new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(Number(value ?? 0));
}

export function dateLabel(value: string | null | undefined): string {
  if (!value) {
    return "—";
  }
  const dateOnly = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  const date = dateOnly
    ? new Date(Number(dateOnly[1]), Number(dateOnly[2]) - 1, Number(dateOnly[3]))
    : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });
}

export function ageLabel(value: string | null | undefined): string {
  if (!value) {
    return "—";
  }
  const then = new Date(value).getTime();
  const delta = Date.now() - then;
  const minutes = Math.max(0, Math.round(delta / 60000));
  if (minutes < 60) {
    return `${minutes}m`;
  }
  const hours = Math.round(minutes / 60);
  if (hours < 48) {
    return `${hours}h`;
  }
  return `${Math.round(hours / 24)}d`;
}

export function humanStatus(status: string): string {
  return status
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function statusTone(status: string): "coral" | "carbon" | "muted" {
  const attention: Array<PurchaseOrderStatus | string> = [
    "PENDING_REVIEW",
    "PENDING_APPROVAL",
    "REVISION_REQUIRED",
    "PENDING_SUPPLIER_CONFIRMATION",
    "PENDING_PROCESS_START",
    "START_FAILED",
    "DELAYED",
  ];
  const done = ["COMPLETED", "RECEIVED", "APPROVED", "ACCEPTED", "DELIVERED", "CONFIRMED"];
  if (attention.includes(status)) {
    return "coral";
  }
  if (done.includes(status)) {
    return "carbon";
  }
  return "muted";
}

export function sourceLabel(initiationType: string): string {
  return initiationType === "REPLENISHMENT" ? "Replenishment" : "Manual";
}

export function taskGroup(elementId: string): "Recommendations" | "Approvals" | "Fulfillment" | "Other" {
  if (elementId.includes("Recommendation")) {
    return "Recommendations";
  }
  if (elementId.includes("Approval") || elementId.includes("Revise")) {
    return "Approvals";
  }
  if (
    elementId.includes("Supplier") ||
    elementId.includes("Shipment") ||
    elementId.includes("GoodsReceipt")
  ) {
    return "Fulfillment";
  }
  return "Other";
}
