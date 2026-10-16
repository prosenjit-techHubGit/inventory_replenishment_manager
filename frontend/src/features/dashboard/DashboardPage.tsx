import { Link } from "react-router-dom";
import { listInventory } from "../../api/catalogApi";
import { listPurchaseOrders } from "../../api/purchaseOrdersApi";
import { listTasks } from "../../api/tasksApi";
import { useRole } from "../../app/RoleContext";
import { dateLabel, money, sourceLabel } from "../../app/format";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { StatusBadge } from "../../components/StatusBadge";

export function DashboardPage() {
  const { role } = useRole();
  const { data, error, loading } = useLoad(async () => {
    const [tasks, orders, inventory] = await Promise.all([
      listTasks(role.id).catch(() => []),
      listPurchaseOrders().catch(() => []),
      listInventory().catch(() => []),
    ]);
    return { tasks, orders, inventory };
  }, [role.id]);

  if (loading) {
    return (
      <>
        <PageHeader title={`Good morning, ${role.name.split(" ")[0]}`} />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title={`Good morning, ${role.name.split(" ")[0]}`} />
        <ErrorBanner message={error} />
      </>
    );
  }

  const belowReorder = data.inventory.filter((item) => item.replenishmentRequired);
  const pendingApproval = data.orders.filter((po) => po.status === "PENDING_APPROVAL");
  const inFulfillment = data.orders.filter((po) =>
    ["IN_FULFILLMENT", "RELEASED", "PARTIALLY_RECEIVED", "SUPPLIER_CONFIRMED"].includes(po.status),
  );
  const recent = [...data.orders]
    .sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
    .slice(0, 5);

  return (
    <>
      <PageHeader
        title={`Good morning, ${role.name.split(" ")[0]}`}
        subtitle={
          belowReorder.length > 0
            ? `${data.tasks.length} tasks waiting. ${belowReorder[0].name} is below reorder.`
            : `${data.tasks.length} tasks waiting.`
        }
        actions={
          <Link className="btn btn-primary" to="/purchase-orders/new">
            Create purchase order
          </Link>
        }
      />
      <ErrorBanner message={error} />
      <section className="metrics">
        <div className="metric">
          <div className="label">Open tasks</div>
          <div className={`value${data.tasks.length ? " alert" : ""}`}>{data.tasks.length}</div>
        </div>
        <div className="metric">
          <div className="label">Pending approval</div>
          <div className="value">{pendingApproval.length}</div>
        </div>
        <div className="metric">
          <div className="label">Below reorder</div>
          <div className={`value${belowReorder.length ? " alert" : ""}`}>{belowReorder.length}</div>
        </div>
        <div className="metric">
          <div className="label">In fulfillment</div>
          <div className="value">{inFulfillment.length}</div>
        </div>
      </section>
      <div className="grid-2">
        <section className="card">
          <h2>Needs attention</h2>
          {data.tasks.length === 0 ? (
            <div className="empty">No open tasks for this role.</div>
          ) : (
            data.tasks.slice(0, 5).map((task) => (
              <div className="list-row" key={task.taskKey}>
                <div>
                  <strong>{task.name}</strong>
                  <div className="muted">{task.businessId ?? "Task"}</div>
                </div>
                <Link className="btn btn-secondary" to={`/tasks/${task.taskKey}`}>
                  Open
                </Link>
              </div>
            ))
          )}
        </section>
        <section className="card pink">
          <h2>Inventory watch</h2>
          {belowReorder.length === 0 ? (
            <div className="empty">All items are above reorder.</div>
          ) : (
            belowReorder.map((item) => {
              const pct = Math.min(100, (Number(item.onHandQuantity) / Number(item.reorderLevel)) * 100);
              return (
                <div key={item.id}>
                  <strong>{item.name}</strong>
                  <div className="muted">
                    {item.sku}
                    {item.criticalItem ? " · Critical item" : ""}
                  </div>
                  <div className="meter">
                    <div style={{ width: `${pct}%` }} />
                  </div>
                  <div className="muted">
                    On hand {item.onHandQuantity} / reorder {item.reorderLevel} · projected{" "}
                    {item.projectedAvailable}
                  </div>
                </div>
              );
            })
          )}
        </section>
      </div>
      <section className="card" style={{ marginTop: 16 }}>
        <h2>Recent purchase orders</h2>
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>PO</th>
                <th>Source</th>
                <th>Status</th>
                <th>Value</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {recent.length === 0 ? (
                <tr>
                  <td colSpan={5} className="muted">
                    No purchase orders yet.
                  </td>
                </tr>
              ) : (
                recent.map((po) => (
                  <tr key={po.purchaseOrderId}>
                    <td>
                      <Link to={`/purchase-orders/${po.purchaseOrderId}`}>{po.poNumber}</Link>
                    </td>
                    <td>{sourceLabel(po.initiationType)}</td>
                    <td>
                      <StatusBadge status={po.status} />
                    </td>
                    <td>{money(po.totalValue)}</td>
                    <td>{dateLabel(po.createdAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
}
