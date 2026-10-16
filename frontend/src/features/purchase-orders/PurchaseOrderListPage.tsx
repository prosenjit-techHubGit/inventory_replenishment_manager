import { Link } from "react-router-dom";
import { listInventory, listSuppliers } from "../../api/catalogApi";
import { listPurchaseOrders } from "../../api/purchaseOrdersApi";
import { dateLabel, money, sourceLabel } from "../../app/format";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { StatusBadge } from "../../components/StatusBadge";

export function PurchaseOrderListPage() {
  const { data, error, loading } = useLoad(async () => {
    const [orders, items, suppliers] = await Promise.all([
      listPurchaseOrders(),
      listInventory(),
      listSuppliers(),
    ]);
    return { orders, items, suppliers };
  }, []);

  const itemName = (id: string) => data?.items.find((item) => item.id === id)?.name ?? id;
  const supplierName = (id: string) => data?.suppliers.find((item) => item.id === id)?.name ?? id;

  return (
    <>
      <PageHeader
        title="Purchase orders"
        actions={
          <Link className="btn btn-primary" to="/purchase-orders/new">
            Create purchase order
          </Link>
        }
      />
      <ErrorBanner message={error} />
      {loading || !data ? (
        <LoadingState />
      ) : (
        <div className="card table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>PO</th>
                <th>Item</th>
                <th>Supplier</th>
                <th>Source</th>
                <th>Status</th>
                <th>Value</th>
                <th>Required</th>
              </tr>
            </thead>
            <tbody>
              {data.orders.length === 0 ? (
                <tr>
                  <td colSpan={7} className="muted">
                    No purchase orders yet.
                  </td>
                </tr>
              ) : (
                data.orders.map((po) => (
                  <tr key={po.purchaseOrderId}>
                    <td>
                      <Link to={`/purchase-orders/${po.purchaseOrderId}`}>{po.poNumber}</Link>
                    </td>
                    <td>{itemName(po.itemId)}</td>
                    <td>{supplierName(po.supplierId)}</td>
                    <td>{sourceLabel(po.initiationType)}</td>
                    <td>
                      <StatusBadge status={po.status} />
                    </td>
                    <td>{money(po.totalValue)}</td>
                    <td>{dateLabel(po.requiredDeliveryDate)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
