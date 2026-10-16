import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "../components/AppShell";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { CreatePurchaseOrderPage } from "../features/purchase-orders/CreatePurchaseOrderPage";
import { PurchaseOrderDetailsPage } from "../features/purchase-orders/PurchaseOrderDetailsPage";
import { PurchaseOrderListPage } from "../features/purchase-orders/PurchaseOrderListPage";
import { RecommendationDetailPage } from "../features/recommendations/RecommendationDetailPage";
import { RecommendationsListPage } from "../features/recommendations/RecommendationsListPage";
import { TaskListPage } from "../features/tasks/TaskListPage";
import { TaskPage } from "../features/tasks/TaskPage";

export function AppRouter() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/tasks" element={<TaskListPage />} />
        <Route path="/tasks/:taskKey" element={<TaskPage />} />
        <Route path="/recommendations" element={<RecommendationsListPage />} />
        <Route path="/recommendations/:id" element={<RecommendationDetailPage />} />
        <Route path="/purchase-orders" element={<PurchaseOrderListPage />} />
        <Route path="/purchase-orders/new" element={<CreatePurchaseOrderPage />} />
        <Route path="/purchase-orders/:id" element={<PurchaseOrderDetailsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
