import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getPurchaseOrder } from "../../api/purchaseOrdersApi";
import { completeTask } from "../../api/tasksApi";
import { dateLabel, money, qty, sourceLabel } from "../../app/format";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { FormField } from "../../components/FormField";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { StatusBadge } from "../../components/StatusBadge";
import { HttpError } from "../../api/httpClient";

interface Props {
  purchaseOrderId: string;
  taskKey: string;
  taskName: string;
  allowedActions: string[];
}

export function ReviewPurchaseOrderPage({ purchaseOrderId, taskKey, taskName, allowedActions }: Props) {
  const navigate = useNavigate();
  const { data, error, loading } = useLoad(() => getPurchaseOrder(purchaseOrderId), [purchaseOrderId]);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  async function submit(action: string) {
    setBusy(true);
    setActionError(null);
    try {
      await completeTask(taskKey, action, { comment }, idempotencyKey);
      navigate("/tasks");
    } catch (err) {
      setActionError(err instanceof HttpError ? err.message : "Unable to complete approval");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <>
        <PageHeader title={taskName} />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title={taskName} />
        <ErrorBanner message={error} />
      </>
    );
  }

  const po = data.purchaseOrder;
  return (
    <>
      <PageHeader
        title="Approve purchase order"
        subtitle={`${po.poNumber} · ${taskName} · revision ${po.poRevision}`}
        actions={<StatusBadge status={po.status} />}
      />
      <ErrorBanner message={error ?? actionError} />
      <div className="grid-form">
        <section className="card">
          <h2>PO facts</h2>
          <dl className="dl">
            <dt>Source</dt>
            <dd>{sourceLabel(po.initiationType)}</dd>
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
        <aside className="summary-rail">
          <h2>{taskName}</h2>
          <FormField label="Comment">
            <textarea value={comment} onChange={(event) => setComment(event.target.value)} />
          </FormField>
          <div className="actions" style={{ flexDirection: "column" }}>
            {allowedActions.includes("APPROVE") ? (
              <button className="btn btn-primary" type="button" disabled={busy} onClick={() => void submit("APPROVE")}>
                Approve
              </button>
            ) : null}
            {allowedActions.includes("RETURN_FOR_REVISION") ? (
              <button
                className="btn btn-secondary"
                type="button"
                disabled={busy}
                onClick={() => void submit("RETURN_FOR_REVISION")}
              >
                Return for revision
              </button>
            ) : null}
            {allowedActions.includes("REJECT") ? (
              <button className="btn btn-danger" type="button" disabled={busy} onClick={() => void submit("REJECT")}>
                Reject
              </button>
            ) : null}
          </div>
          <p className="muted">
            Return sends this to the buyer to edit. Approvals restart. Reject ends the PO.
          </p>
        </aside>
      </div>
    </>
  );
}
