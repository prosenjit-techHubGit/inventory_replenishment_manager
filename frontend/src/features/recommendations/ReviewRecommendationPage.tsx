import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { listInventory, listSuppliers } from "../../api/catalogApi";
import { getRecommendation } from "../../api/recommendationsApi";
import { completeTask } from "../../api/tasksApi";
import { qty } from "../../app/format";
import { useToast } from "../../app/ToastContext";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { FormField } from "../../components/FormField";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { StatusBadge } from "../../components/StatusBadge";
import { HttpError } from "../../api/httpClient";

interface Props {
  recommendationId: string;
  taskKey?: string;
  allowedActions?: string[];
}

function recommendationMessage(action: string): string {
  if (action === "ACCEPT") {
    return "Recommendation accepted successfully.";
  }
  if (action === "MODIFY_AND_ACCEPT") {
    return "Recommendation updated and accepted successfully.";
  }
  if (action === "REJECT") {
    return "Recommendation rejected.";
  }
  return "Task submitted successfully.";
}

export function ReviewRecommendationPage({ recommendationId, taskKey, allowedActions = [] }: Props) {
  const navigate = useNavigate();
  const { notifySuccess } = useToast();
  const { data, error, loading } = useLoad(async () => {
    const [recommendation, items, suppliers] = await Promise.all([
      getRecommendation(recommendationId),
      listInventory(),
      listSuppliers(),
    ]);
    return { recommendation, items, suppliers };
  }, [recommendationId]);
  const [quantity, setQuantity] = useState<string | null>(null);
  const [supplierId, setSupplierId] = useState<string | null>(null);
  const [targetDeliveryDate, setTargetDeliveryDate] = useState<string | null>(null);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  async function submit(action: string) {
    if (!taskKey) {
      return;
    }
    setBusy(true);
    setActionError(null);
    try {
      await completeTask(
        taskKey,
        action,
        {
          comment,
          recommendedQuantity: Number(quantity ?? data?.recommendation.recommendedQuantity),
          supplierId: supplierId ?? data?.recommendation.supplierId,
          targetDeliveryDate: targetDeliveryDate ?? data?.recommendation.targetDeliveryDate,
        },
        idempotencyKey,
      );
      notifySuccess(recommendationMessage(action));
      navigate("/tasks");
    } catch (err) {
      setActionError(err instanceof HttpError ? err.message : "Unable to complete task");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <>
        <PageHeader title="Review recommendation" />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title="Review recommendation" />
        <ErrorBanner message={error} />
      </>
    );
  }

  const rec = data.recommendation;
  const item = data.items.find((entry) => entry.id === rec.itemId);
  const qtyValue = quantity ?? String(rec.recommendedQuantity);
  const supplierValue = supplierId ?? rec.supplierId;
  const dateValue = targetDeliveryDate ?? rec.targetDeliveryDate;
  const canAct = Boolean(taskKey) && rec.status === "PENDING_REVIEW";
  const fillPct = item
    ? Math.min(100, (Number(item.onHandQuantity) / Number(item.reorderLevel)) * 100)
    : 0;

  return (
    <>
      <PageHeader
        title="Review recommendation"
        subtitle="Scheduled replenishment · AI-generated proposal"
        actions={<StatusBadge status={rec.status} />}
      />
      <ErrorBanner message={error ?? actionError} />
      <div className="grid-form">
        <div>
          <section className="card" style={{ marginBottom: 16 }}>
            <h2>Inventory context</h2>
            <dl className="dl">
              <dt>Item</dt>
              <dd>
                {item?.name ?? rec.itemId} · {item?.sku}
                {item?.criticalItem ? " · Critical" : ""}
              </dd>
              <dt>On hand</dt>
              <dd>{qty(item?.onHandQuantity ?? rec.currentStock)}</dd>
              <dt>Reorder</dt>
              <dd>{qty(item?.reorderLevel)}</dd>
              <dt>Open PO qty</dt>
              <dd>{qty(item?.openPurchaseQuantity)}</dd>
              <dt>Projected</dt>
              <dd>{qty(item?.projectedAvailable)}</dd>
            </dl>
            {item ? (
              <div className="meter">
                <div style={{ width: `${fillPct}%` }} />
              </div>
            ) : null}
            <p className="muted">Projected = on hand + open purchase quantity.</p>
          </section>
          <section className="card">
            <h2>Proposed order</h2>
            <FormField label="Quantity">
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={qtyValue}
                disabled={!canAct}
                onChange={(event) => setQuantity(event.target.value)}
              />
            </FormField>
            <FormField label="Supplier">
              <select
                value={supplierValue}
                disabled={!canAct}
                onChange={(event) => setSupplierId(event.target.value)}
              >
                {data.suppliers
                  .filter((supplier) => supplier.active)
                  .map((supplier) => (
                    <option key={supplier.id} value={supplier.id}>
                      {supplier.name} · {supplier.riskLevel} · {supplier.leadTimeDays}d
                    </option>
                  ))}
              </select>
            </FormField>
            <FormField label="Target delivery date">
              <input
                type="date"
                value={dateValue}
                disabled={!canAct}
                onChange={(event) => setTargetDeliveryDate(event.target.value)}
              />
            </FormField>
            <p>
              <StatusBadge status={rec.riskLevel} />
            </p>
            <p>{rec.rationale}</p>
          </section>
        </div>
        <aside className="summary-rail">
          <h2>Decision</h2>
          {canAct ? (
            <>
              <FormField label="Comment">
                <textarea value={comment} onChange={(event) => setComment(event.target.value)} />
              </FormField>
              <div className="actions" style={{ flexDirection: "column" }}>
                {allowedActions.includes("ACCEPT") ? (
                  <button className="btn btn-primary" type="button" disabled={busy} onClick={() => void submit("ACCEPT")}>
                    Accept recommendation
                  </button>
                ) : null}
                {allowedActions.includes("MODIFY_AND_ACCEPT") ? (
                  <button
                    className="btn btn-secondary"
                    type="button"
                    disabled={busy}
                    onClick={() => void submit("MODIFY_AND_ACCEPT")}
                  >
                    Modify and accept
                  </button>
                ) : null}
                {allowedActions.includes("REJECT") ? (
                  <button className="btn btn-danger" type="button" disabled={busy} onClick={() => void submit("REJECT")}>
                    Reject
                  </button>
                ) : null}
              </div>
              <p className="muted">
                Accept starts a purchase order with these values. You will not re-enter the PO form.
                Reject ends this recommendation.
              </p>
            </>
          ) : (
            <p className="muted">
              {taskKey
                ? "This recommendation is no longer pending review."
                : "Complete this from Inbox when a review task is assigned."}
            </p>
          )}
          <p>
            <Link to="/recommendations">Back to recommendations</Link>
          </p>
        </aside>
      </div>
    </>
  );
}
