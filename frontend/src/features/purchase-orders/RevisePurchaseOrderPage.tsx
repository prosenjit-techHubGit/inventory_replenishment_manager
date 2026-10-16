import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { listSuppliers } from "../../api/catalogApi";
import { getPurchaseOrder } from "../../api/purchaseOrdersApi";
import { completeTask } from "../../api/tasksApi";
import { money } from "../../app/format";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { FormField } from "../../components/FormField";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { HttpError } from "../../api/httpClient";
import type { Urgency } from "../../models/types";

interface Props {
  purchaseOrderId: string;
  taskKey: string;
}

export function RevisePurchaseOrderPage({ purchaseOrderId, taskKey }: Props) {
  const navigate = useNavigate();
  const { data, error, loading } = useLoad(async () => {
    const [detail, suppliers] = await Promise.all([getPurchaseOrder(purchaseOrderId), listSuppliers()]);
    return { detail, suppliers: suppliers.filter((supplier) => supplier.active) };
  }, [purchaseOrderId]);
  const [quantity, setQuantity] = useState<string | null>(null);
  const [supplierId, setSupplierId] = useState<string | null>(null);
  const [unitPrice, setUnitPrice] = useState<string | null>(null);
  const [requiredDeliveryDate, setRequiredDeliveryDate] = useState<string | null>(null);
  const [urgency, setUrgency] = useState<Urgency | null>(null);
  const [businessJustification, setBusinessJustification] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  async function onSubmit() {
    if (!data) {
      return;
    }
    const po = data.detail.purchaseOrder;
    setBusy(true);
    setActionError(null);
    try {
      await completeTask(
        taskKey,
        "SUBMIT_REVISION",
        {
          quantity: Number(quantity ?? po.quantity),
          supplierId: supplierId ?? po.supplierId,
          unitPrice: Number(unitPrice ?? po.unitPrice),
          requiredDeliveryDate: requiredDeliveryDate ?? po.requiredDeliveryDate,
          urgency: urgency ?? po.urgency,
          businessJustification: businessJustification ?? po.businessJustification,
        },
        idempotencyKey,
      );
      navigate("/tasks");
    } catch (err) {
      setActionError(err instanceof HttpError ? err.message : "Unable to submit revision");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <>
        <PageHeader title="Revise purchase order" />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title="Revise purchase order" />
        <ErrorBanner message={error} />
      </>
    );
  }

  const po = data.detail.purchaseOrder;
  const qtyValue = quantity ?? String(po.quantity);
  const priceValue = unitPrice ?? String(po.unitPrice);
  const total = Number(qtyValue) * Number(priceValue);

  return (
    <>
      <PageHeader
        title="Revise purchase order"
        subtitle={`${po.poNumber} · revision will increment after submit`}
      />
      <ErrorBanner message={error ?? actionError} />
      <div className="grid-form">
        <section className="card">
          <FormField label="Quantity">
            <input type="number" min="0.01" step="0.01" value={qtyValue} onChange={(event) => setQuantity(event.target.value)} />
          </FormField>
          <FormField label="Supplier">
            <select value={supplierId ?? po.supplierId} onChange={(event) => setSupplierId(event.target.value)}>
              {data.suppliers.map((supplier) => (
                <option key={supplier.id} value={supplier.id}>
                  {supplier.name}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Unit price">
            <input type="number" min="0" step="0.01" value={priceValue} onChange={(event) => setUnitPrice(event.target.value)} />
          </FormField>
          <FormField label="Required delivery date">
            <input
              type="date"
              value={requiredDeliveryDate ?? po.requiredDeliveryDate}
              onChange={(event) => setRequiredDeliveryDate(event.target.value)}
            />
          </FormField>
          <FormField label="Urgency">
            <div className="segmented">
              {(["NORMAL", "HIGH", "URGENT"] as Urgency[]).map((value) => (
                <button
                  key={value}
                  type="button"
                  className={(urgency ?? po.urgency) === value ? "active" : ""}
                  onClick={() => setUrgency(value)}
                >
                  {value}
                </button>
              ))}
            </div>
          </FormField>
          <FormField label="Business justification">
            <textarea
              value={businessJustification ?? po.businessJustification ?? ""}
              onChange={(event) => setBusinessJustification(event.target.value)}
            />
          </FormField>
          <div className="actions">
            <button className="btn btn-primary" type="button" disabled={busy} onClick={() => void onSubmit()}>
              Submit revision
            </button>
          </div>
        </section>
        <aside className="summary-rail">
          <div className="muted">Revised total</div>
          <div className="summary-total">{money(total)}</div>
          <p className="muted">Approvals restart from Procurement Manager for the new revision.</p>
        </aside>
      </div>
    </>
  );
}
