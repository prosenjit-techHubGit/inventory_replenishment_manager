import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { getPurchaseOrder } from "../../api/purchaseOrdersApi";
import { completeTask } from "../../api/tasksApi";
import { qty } from "../../app/format";
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
}

export function GoodsReceiptPage({ purchaseOrderId, taskKey }: Props) {
  const navigate = useNavigate();
  const { data, error, loading } = useLoad(() => getPurchaseOrder(purchaseOrderId), [purchaseOrderId]);
  const [receivedQuantity, setReceivedQuantity] = useState<string | null>(null);
  const [acceptedQuantity, setAcceptedQuantity] = useState<string | null>(null);
  const [rejectedQuantity, setRejectedQuantity] = useState("0");
  const [receiptDate, setReceiptDate] = useState(new Date().toISOString().slice(0, 10));
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  async function onSubmit() {
    if (!data) {
      return;
    }
    const remaining = Number(data.remainingQuantity);
    const received = Number(receivedQuantity ?? remaining);
    const accepted = Number(acceptedQuantity ?? remaining);
    const rejected = Number(rejectedQuantity);
    if (!(received > 0) || accepted < 0 || rejected < 0 || accepted + rejected > received) {
      setActionError("accepted + rejected cannot exceed received, and received must be positive.");
      return;
    }
    setBusy(true);
    setActionError(null);
    try {
      await completeTask(
        taskKey,
        "RECORD_RECEIPT",
        { receivedQuantity: received, acceptedQuantity: accepted, rejectedQuantity: rejected, receiptDate, notes },
        idempotencyKey,
      );
      navigate("/tasks");
    } catch (err) {
      setActionError(err instanceof HttpError ? err.message : "Unable to record receipt");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <>
        <PageHeader title="Record goods receipt" />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title="Record goods receipt" />
        <ErrorBanner message={error} />
      </>
    );
  }

  const po = data.purchaseOrder;
  const remaining = Number(data.remainingQuantity);
  const received = Number(receivedQuantity ?? remaining);
  const accepted = Number(acceptedQuantity ?? remaining);

  return (
    <>
      <PageHeader
        title="Record goods receipt"
        subtitle={po.poNumber}
        actions={<StatusBadge status={po.status} />}
      />
      <ErrorBanner message={error ?? actionError} />
      <div className="grid-form">
        <section className="card">
          <dl className="dl">
            <dt>Item</dt>
            <dd>{data.itemName}</dd>
            <dt>Ordered</dt>
            <dd>{qty(po.quantity)}</dd>
            <dt>Already accepted</dt>
            <dd>{qty(data.cumulativeAcceptedQuantity)}</dd>
            <dt>Remaining</dt>
            <dd>{qty(remaining)}</dd>
          </dl>
          <div className="meter">
            <div
              style={{
                width: `${Math.min(100, (Number(data.cumulativeAcceptedQuantity) / Number(po.quantity)) * 100)}%`,
              }}
            />
          </div>
          <p className="muted">Remaining is derived. Partial receipt keeps the PO open.</p>
        </section>
        <aside className="summary-rail">
          <FormField label="Received quantity *">
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={receivedQuantity ?? String(remaining)}
              onChange={(event) => setReceivedQuantity(event.target.value)}
            />
          </FormField>
          <FormField label="Accepted quantity *">
            <input
              type="number"
              min="0"
              step="0.01"
              value={acceptedQuantity ?? String(remaining)}
              onChange={(event) => setAcceptedQuantity(event.target.value)}
            />
          </FormField>
          <FormField label="Rejected quantity">
            <input
              type="number"
              min="0"
              step="0.01"
              value={rejectedQuantity}
              onChange={(event) => setRejectedQuantity(event.target.value)}
            />
          </FormField>
          <FormField label="Receipt date *">
            <input type="date" value={receiptDate} onChange={(event) => setReceiptDate(event.target.value)} />
          </FormField>
          <FormField label="Notes">
            <textarea value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Condition, discrepancies" />
          </FormField>
          <button className="btn btn-primary" type="button" disabled={busy} onClick={() => void onSubmit()}>
            Record receipt
          </button>
          <p className="muted">
            Recording {qty(accepted)} accepted of {qty(received)} received.
          </p>
        </aside>
      </div>
    </>
  );
}
