import { useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { listInventory, listSuppliers } from "../../api/catalogApi";
import { createPurchaseOrder } from "../../api/purchaseOrdersApi";
import { money } from "../../app/format";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { FormField } from "../../components/FormField";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { HttpError } from "../../api/httpClient";
import type { Urgency } from "../../models/types";

export function CreatePurchaseOrderPage() {
  const navigate = useNavigate();
  const { data, error, loading } = useLoad(async () => {
    const [items, suppliers] = await Promise.all([listInventory(), listSuppliers()]);
    return { items, suppliers: suppliers.filter((supplier) => supplier.active) };
  }, []);
  const [itemId, setItemId] = useState("");
  const [supplierId, setSupplierId] = useState("");
  const [quantity, setQuantity] = useState("40");
  const [unitPrice, setUnitPrice] = useState("");
  const [requiredDeliveryDate, setRequiredDeliveryDate] = useState("");
  const [urgency, setUrgency] = useState<Urgency>("NORMAL");
  const [businessJustification, setBusinessJustification] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  const selectedSupplier = data?.suppliers.find((supplier) => supplier.id === supplierId);

  const total = useMemo(() => {
    const qty = Number(quantity);
    const price = Number(unitPrice || selectedSupplier?.unitPrice || 0);
    return Number.isFinite(qty) && Number.isFinite(price) ? qty * price : 0;
  }, [quantity, unitPrice, selectedSupplier]);

  function onSupplierChange(id: string) {
    setSupplierId(id);
    const supplier = data?.suppliers.find((item) => item.id === id);
    if (supplier) {
      setUnitPrice(String(supplier.unitPrice));
    }
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    const qty = Number(quantity);
    const price = Number(unitPrice);
    if (!itemId || !supplierId || !requiredDeliveryDate) {
      setFormError("Item, supplier and delivery date are required.");
      return;
    }
    if (!(qty > 0) || price < 0) {
      setFormError("Quantity must be positive and price cannot be negative.");
      return;
    }
    setSubmitting(true);
    try {
      const created = await createPurchaseOrder(
        {
          itemId,
          supplierId,
          quantity: qty,
          unitPrice: price,
          requiredDeliveryDate,
          urgency,
          businessJustification,
        },
        idempotencyKey,
      );
      navigate(`/purchase-orders/${created.purchaseOrderId}`);
    } catch (err) {
      setFormError(err instanceof HttpError ? err.message : "Unable to create purchase order");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <>
        <PageHeader title="Create purchase order" />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title="Create purchase order" />
        <ErrorBanner message={error} />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Create purchase order"
        subtitle="Manual path. This does not run replenishment or AI recommendation."
      />
      <ErrorBanner message={error ?? formError} />
      <form className="grid-form" onSubmit={onSubmit}>
        <section className="card">
          <FormField label="Item *">
            <select value={itemId} onChange={(event) => setItemId(event.target.value)} required>
              <option value="">Select item</option>
              {data.items.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name} ({item.sku})
                </option>
              ))}
            </select>
          </FormField>
          <div className="form-grid-2">
            <FormField label="Quantity *">
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={quantity}
                onChange={(event) => setQuantity(event.target.value)}
                required
              />
            </FormField>
            <FormField
              label="Supplier *"
              hint={
                selectedSupplier
                  ? `Lead time ${selectedSupplier.leadTimeDays} days · risk ${selectedSupplier.riskLevel} · ${money(selectedSupplier.unitPrice)}`
                  : undefined
              }
            >
              <select value={supplierId} onChange={(event) => onSupplierChange(event.target.value)} required>
                <option value="">Select supplier</option>
                {data.suppliers.map((supplier) => (
                  <option key={supplier.id} value={supplier.id}>
                    {supplier.name}
                  </option>
                ))}
              </select>
            </FormField>
          </div>
          <div className="form-grid-2">
            <FormField label="Unit price *">
              <input
                type="number"
                min="0"
                step="0.01"
                value={unitPrice}
                onChange={(event) => setUnitPrice(event.target.value)}
                required
              />
            </FormField>
            <FormField label="Required delivery date *">
              <input
                type="date"
                value={requiredDeliveryDate}
                onChange={(event) => setRequiredDeliveryDate(event.target.value)}
                required
              />
            </FormField>
          </div>
          <FormField label="Urgency *">
            <div className="segmented">
              {(["NORMAL", "HIGH", "URGENT"] as Urgency[]).map((value) => (
                <button
                  key={value}
                  type="button"
                  className={urgency === value ? "active" : ""}
                  onClick={() => setUrgency(value)}
                >
                  {value}
                </button>
              ))}
            </div>
          </FormField>
          <FormField label="Business justification">
            <textarea
              value={businessJustification}
              onChange={(event) => setBusinessJustification(event.target.value)}
            />
          </FormField>
          <div className="actions">
            <Link className="btn btn-ghost" to="/purchase-orders">
              Cancel
            </Link>
            <button className="btn btn-primary" type="submit" disabled={submitting}>
              {submitting ? "Creating…" : "Create purchase order"}
            </button>
          </div>
        </section>
        <aside className="summary-rail">
          <div className="muted">Estimated total</div>
          <div className="summary-total">{money(total)}</div>
          <p className="muted">
            Initiation type Manual. A PO number is assigned on submit. Process start is asynchronous
            — the order may briefly show as starting.
          </p>
        </aside>
      </form>
    </>
  );
}
