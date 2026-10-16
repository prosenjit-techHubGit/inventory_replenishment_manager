import { Link, useParams } from "react-router-dom";
import { getPurchaseOrder } from "../../api/purchaseOrdersApi";
import { dateLabel, money, qty, sourceLabel } from "../../app/format";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { StatusBadge } from "../../components/StatusBadge";
import type { PurchaseOrderStatus } from "../../models/types";

const STEPS: { key: string; match: PurchaseOrderStatus[] }[] = [
  { key: "Prepared", match: ["IN_PREPARATION", "PENDING_PROCESS_START", "DRAFT"] },
  { key: "Approved", match: ["PENDING_APPROVAL", "REVISION_REQUIRED", "APPROVED"] },
  { key: "Supplier confirmed", match: ["PENDING_SUPPLIER_CONFIRMATION", "SUPPLIER_DATE_REVISED", "SUPPLIER_CONFIRMED"] },
  { key: "Released", match: ["RELEASED"] },
  { key: "In fulfillment", match: ["IN_FULFILLMENT", "PARTIALLY_RECEIVED"] },
  { key: "Goods receipt", match: ["RECEIVED"] },
  { key: "Complete", match: ["COMPLETED"] },
];

function stepState(status: PurchaseOrderStatus, index: number): "done" | "current" | "" {
  if (["REJECTED", "SUPPLIER_REJECTED", "CANCELLED", "START_FAILED"].includes(status)) {
    return "";
  }
  const currentIndex = Math.max(
    0,
    STEPS.findIndex((step) => step.match.includes(status)),
  );
  if (index < currentIndex) {
    return "done";
  }
  if (index === currentIndex) {
    return "current";
  }
  return "";
}

export function PurchaseOrderDetailsPage() {
  const { id = "" } = useParams();
  const { data, error, loading } = useLoad(() => getPurchaseOrder(id), [id]);

  if (loading) {
    return (
      <>
        <PageHeader title="Purchase order" />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title="Purchase order" />
        <ErrorBanner message={error} />
      </>
    );
  }

  const po = data.purchaseOrder;
  const ordered = Number(po.quantity);
  const accepted = Number(data.cumulativeAcceptedQuantity);
  const remaining = Number(data.remainingQuantity);
  const fillPct = ordered === 0 ? 0 : Math.min(100, (accepted / ordered) * 100);

  return (
    <>
      <PageHeader
        title={po.poNumber}
        subtitle={`${data.itemName ?? po.itemId} · ${sourceLabel(po.initiationType)}`}
        actions={<StatusBadge status={po.status} />}
      />
      <ErrorBanner message={error} />
      <div className="stepper">
        {STEPS.map((step, index) => (
          <div key={step.key} className={`step ${stepState(po.status, index)}`}>
            <span>{step.key}</span>
          </div>
        ))}
      </div>
      <div className="grid-2">
        <section className="card">
          <h2>Order summary</h2>
          <dl className="dl">
            <dt>Item</dt>
            <dd>
              {data.itemName} · {data.itemSku}
            </dd>
            <dt>Quantity</dt>
            <dd>{qty(po.quantity)}</dd>
            <dt>Supplier</dt>
            <dd>
              {data.supplierName} · risk {po.supplierRisk}
            </dd>
            <dt>Unit price</dt>
            <dd>{money(po.unitPrice)}</dd>
            <dt>Total</dt>
            <dd>{money(po.totalValue)}</dd>
            <dt>Required</dt>
            <dd>{dateLabel(po.requiredDeliveryDate)}</dd>
            <dt>Urgency</dt>
            <dd>{po.urgency}</dd>
            <dt>Revision</dt>
            <dd>{po.poRevision}</dd>
            {po.recommendationId ? (
              <>
                <dt>Recommendation</dt>
                <dd>
                  <Link to={`/recommendations/${po.recommendationId}`}>{po.recommendationId}</Link>
                </dd>
              </>
            ) : null}
            <dt>Justification</dt>
            <dd>{po.businessJustification || "—"}</dd>
          </dl>
        </section>
        <section className="card">
          <h2>Receipts</h2>
          <div className="muted">
            Ordered {qty(ordered)} · Accepted {qty(accepted)} · Remaining {qty(remaining)}
          </div>
          <div className="meter">
            <div style={{ width: `${fillPct}%` }} />
          </div>
          {data.receipts.length === 0 ? (
            <p className="muted">No receipts yet. The PO stays open until remaining is 0.</p>
          ) : (
            data.receipts.map((receipt) => (
              <div className="list-row" key={receipt.id}>
                <div>
                  GRN-{receipt.receiptNumber}
                  <div className="muted">
                    Accepted {qty(receipt.acceptedQuantity)} on {dateLabel(receipt.receiptDate)}
                  </div>
                </div>
              </div>
            ))
          )}
        </section>
      </div>
      <div className="grid-2" style={{ marginTop: 16 }}>
        <section className="card">
          <h2>Shipments</h2>
          {data.shipments.length === 0 ? (
            <p className="muted">No shipments yet.</p>
          ) : (
            data.shipments.map((shipment) => (
              <div className="list-row" key={shipment.id}>
                <div>
                  {shipment.supplierShipmentReference ?? "Shipment"}
                  <div className="muted">
                    Planned {dateLabel(shipment.plannedDeliveryDate)}
                    {shipment.trackingReference ? ` · ${shipment.trackingReference}` : ""}
                  </div>
                </div>
                <StatusBadge status={shipment.status} />
              </div>
            ))
          )}
        </section>
        <section className="card">
          <h2>Approvals</h2>
          {data.approvals.length === 0 ? (
            <p className="muted">No approval records on this revision yet.</p>
          ) : (
            <table className="data">
              <thead>
                <tr>
                  <th>Role</th>
                  <th>Rev</th>
                  <th>Outcome</th>
                </tr>
              </thead>
              <tbody>
                {data.approvals.map((approval) => (
                  <tr key={approval.id}>
                    <td>{approval.approvalRole.replaceAll("_", " ")}</td>
                    <td>{approval.poRevision}</td>
                    <td>{approval.outcome ? <StatusBadge status={approval.outcome} /> : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </>
  );
}
