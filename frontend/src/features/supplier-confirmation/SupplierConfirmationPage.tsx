import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { getPurchaseOrder } from "../../api/purchaseOrdersApi";
import { completeTask } from "../../api/tasksApi";
import { dateLabel, money, qty } from "../../app/format";
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
  allowedActions: string[];
}

export function SupplierConfirmationPage({ purchaseOrderId, taskKey, allowedActions }: Props) {
  const navigate = useNavigate();
  const { data, error, loading } = useLoad(() => getPurchaseOrder(purchaseOrderId), [purchaseOrderId]);
  const [status, setStatus] = useState<"CONFIRMED" | "REVISED_DATE" | "REJECTED">("CONFIRMED");
  const [revisedDeliveryDate, setRevisedDeliveryDate] = useState("");
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  async function onSubmit() {
    if (!data) {
      return;
    }
    if (status === "REVISED_DATE" && !revisedDeliveryDate) {
      setActionError("A revised delivery date is required.");
      return;
    }
    setBusy(true);
    setActionError(null);
    try {
      await completeTask(
        taskKey,
        status,
        {
          comment,
          revisedDeliveryDate: status === "REVISED_DATE" ? revisedDeliveryDate : data.purchaseOrder.requiredDeliveryDate,
        },
        idempotencyKey,
      );
      navigate("/tasks");
    } catch (err) {
      setActionError(err instanceof HttpError ? err.message : "Unable to submit confirmation");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <>
        <PageHeader title="Confirm purchase order" />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title="Confirm purchase order" />
        <ErrorBanner message={error} />
      </>
    );
  }

  const po = data.purchaseOrder;
  const choices = [
    { id: "CONFIRMED" as const, title: "Confirmed", hint: "We can fulfill as requested" },
    { id: "REVISED_DATE" as const, title: "Revised date", hint: "Propose a new delivery date" },
    { id: "REJECTED" as const, title: "Rejected", hint: "Cannot fulfill" },
  ].filter((choice) => allowedActions.includes(choice.id));

  return (
    <>
      <PageHeader
        title="Confirm purchase order"
        subtitle={po.poNumber}
        actions={<StatusBadge status={po.status} />}
      />
      <ErrorBanner message={error ?? actionError} />
      <div className="grid-form">
        <section className="card">
          <h2>Order</h2>
          <dl className="dl">
            <dt>Supplier</dt>
            <dd>{data.supplierName}</dd>
            <dt>Item</dt>
            <dd>{data.itemName}</dd>
            <dt>Quantity</dt>
            <dd>{qty(po.quantity)}</dd>
            <dt>Requested</dt>
            <dd>{dateLabel(po.requiredDeliveryDate)}</dd>
            <dt>Unit price</dt>
            <dd>{money(po.unitPrice)}</dd>
          </dl>
        </section>
        <aside className="summary-rail">
          <h2>Confirmation</h2>
          <div className="choice-stack">
            {choices.map((choice) => (
              <button
                key={choice.id}
                type="button"
                className={`choice${status === choice.id ? " active" : ""}`}
                onClick={() => setStatus(choice.id)}
              >
                <strong>{choice.title}</strong>
                <span className="muted">{choice.hint}</span>
              </button>
            ))}
          </div>
          {status === "REVISED_DATE" ? (
            <FormField label="Confirmed delivery date">
              <input
                type="date"
                value={revisedDeliveryDate}
                onChange={(event) => setRevisedDeliveryDate(event.target.value)}
              />
            </FormField>
          ) : null}
          <FormField label="Comment">
            <textarea value={comment} onChange={(event) => setComment(event.target.value)} />
          </FormField>
          <button className="btn btn-primary" type="button" disabled={busy} onClick={() => void onSubmit()}>
            Submit confirmation
          </button>
          <p className="muted">
            Confirmed releases the order. Revised date updates delivery. Rejected is a terminal supplier
            exception.
          </p>
        </aside>
      </div>
    </>
  );
}
