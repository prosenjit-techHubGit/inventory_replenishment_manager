import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { getPurchaseOrder } from "../../api/purchaseOrdersApi";
import { completeTask } from "../../api/tasksApi";
import { dateLabel } from "../../app/format";
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

export function TrackShipmentPage({ purchaseOrderId, taskKey }: Props) {
  const navigate = useNavigate();
  const { data, error, loading } = useLoad(() => getPurchaseOrder(purchaseOrderId), [purchaseOrderId]);
  const [trackingReference, setTrackingReference] = useState("");
  const [actualDeliveryDate, setActualDeliveryDate] = useState(new Date().toISOString().slice(0, 10));
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  async function onSubmit() {
    setBusy(true);
    setActionError(null);
    try {
      await completeTask(
        taskKey,
        "DELIVERED",
        { trackingReference, actualDeliveryDate },
        idempotencyKey,
      );
      navigate("/tasks");
    } catch (err) {
      setActionError(err instanceof HttpError ? err.message : "Unable to record delivery");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <>
        <PageHeader title="Record delivery" />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title="Record delivery" />
        <ErrorBanner message={error} />
      </>
    );
  }

  const shipment = data.shipments[0];
  return (
    <>
      <PageHeader
        title="Record delivery"
        subtitle={data.purchaseOrder.poNumber}
        actions={shipment ? <StatusBadge status={shipment.status} /> : null}
      />
      <ErrorBanner message={error ?? actionError} />
      <div className="grid-form">
        <section className="card">
          <dl className="dl">
            <dt>Item</dt>
            <dd>{data.itemName}</dd>
            <dt>Shipment</dt>
            <dd>{shipment?.supplierShipmentReference ?? "—"}</dd>
            <dt>Planned</dt>
            <dd>{dateLabel(shipment?.plannedDeliveryDate)}</dd>
          </dl>
        </section>
        <aside className="summary-rail">
          <FormField label="Tracking reference">
            <input value={trackingReference} onChange={(event) => setTrackingReference(event.target.value)} />
          </FormField>
          <FormField label="Actual delivery date">
            <input type="date" value={actualDeliveryDate} onChange={(event) => setActualDeliveryDate(event.target.value)} />
          </FormField>
          <button className="btn btn-primary" type="button" disabled={busy} onClick={() => void onSubmit()}>
            Record delivery
          </button>
        </aside>
      </div>
    </>
  );
}
