import { Link } from "react-router-dom";
import { listInventory, listSuppliers } from "../../api/catalogApi";
import { listRecommendations } from "../../api/recommendationsApi";
import { dateLabel, qty } from "../../app/format";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { StatusBadge } from "../../components/StatusBadge";

export function RecommendationsListPage() {
  const { data, error, loading } = useLoad(async () => {
    const [recommendations, items, suppliers] = await Promise.all([
      listRecommendations(),
      listInventory(),
      listSuppliers(),
    ]);
    return { recommendations, items, suppliers };
  }, []);

  const itemName = (id: string) => data?.items.find((item) => item.id === id)?.name ?? id;
  const supplierName = (id: string) => data?.suppliers.find((item) => item.id === id)?.name ?? id;

  return (
    <>
      <PageHeader title="Recommendations" subtitle="AI and fallback replenishment proposals." />
      <ErrorBanner message={error} />
      {loading || !data ? (
        <LoadingState />
      ) : (
        <div className="card table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>Item</th>
                <th>Qty</th>
                <th>Supplier</th>
                <th>Risk</th>
                <th>Delivery</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {data.recommendations.length === 0 ? (
                <tr>
                  <td colSpan={6} className="muted">
                    No recommendations yet.
                  </td>
                </tr>
              ) : (
                data.recommendations.map((rec) => (
                  <tr key={rec.recommendationId}>
                    <td>
                      <Link to={`/recommendations/${rec.recommendationId}`}>{itemName(rec.itemId)}</Link>
                    </td>
                    <td>{qty(rec.recommendedQuantity)}</td>
                    <td>{supplierName(rec.supplierId)}</td>
                    <td>
                      <StatusBadge status={rec.riskLevel} />
                    </td>
                    <td>{dateLabel(rec.targetDeliveryDate)}</td>
                    <td>
                      <StatusBadge status={rec.status} />
                    </td>
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
