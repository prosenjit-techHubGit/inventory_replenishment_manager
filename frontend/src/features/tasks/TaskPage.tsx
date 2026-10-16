import { useParams } from "react-router-dom";
import { listPurchaseOrders } from "../../api/purchaseOrdersApi";
import { claimTask, getTask } from "../../api/tasksApi";
import { useLoad } from "../../app/useLoad";
import { ErrorBanner } from "../../components/ErrorBanner";
import { LoadingState } from "../../components/LoadingState";
import { PageHeader } from "../../components/PageHeader";
import { GoodsReceiptPage } from "../goods-receipt/GoodsReceiptPage";
import { TrackShipmentPage } from "../goods-receipt/TrackShipmentPage";
import { ReviewPurchaseOrderPage } from "../purchase-orders/ReviewPurchaseOrderPage";
import { RevisePurchaseOrderPage } from "../purchase-orders/RevisePurchaseOrderPage";
import { ReviewRecommendationPage } from "../recommendations/ReviewRecommendationPage";
import { SupplierConfirmationPage } from "../supplier-confirmation/SupplierConfirmationPage";

function asString(value: unknown): string | null {
  return value === undefined || value === null ? null : String(value);
}

export function TaskPage() {
  const { taskKey = "" } = useParams();
  const { data, error, loading } = useLoad(async () => {
    const task = await getTask(taskKey);
    await claimTask(taskKey).catch(() => undefined);
    let purchaseOrderId = asString(task.data.purchaseOrderId);
    if (!purchaseOrderId && task.businessId) {
      const orders = await listPurchaseOrders();
      purchaseOrderId =
        orders.find((order) => order.poRequestId === task.businessId)?.purchaseOrderId ??
        orders.find((order) => order.poNumber === task.businessId)?.purchaseOrderId ??
        null;
    }
    return {
      task,
      purchaseOrderId,
      recommendationId: asString(task.data.recommendationId),
    };
  }, [taskKey]);

  if (loading) {
    return (
      <>
        <PageHeader title="Task" />
        <LoadingState />
      </>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title="Task" />
        <ErrorBanner message={error} />
      </>
    );
  }

  const { task, purchaseOrderId, recommendationId } = data;
  const type = task.taskType;

  if (type === "UserTask_ReviewRecommendation" && recommendationId) {
    return (
      <ReviewRecommendationPage
        recommendationId={recommendationId}
        taskKey={task.taskKey}
        allowedActions={task.allowedActions}
      />
    );
  }
  if (!purchaseOrderId && type !== "UserTask_ReviewRecommendation") {
    return (
      <>
        <PageHeader title={task.name} />
        <ErrorBanner message="This task is not linked to a purchase order yet." />
      </>
    );
  }
  if (
    type === "UserTask_ProcurementApproval" ||
    type === "UserTask_FinanceApproval" ||
    type === "UserTask_SupplyChainApproval"
  ) {
    return (
      <ReviewPurchaseOrderPage
        purchaseOrderId={purchaseOrderId!}
        taskKey={task.taskKey}
        taskName={task.name}
        allowedActions={task.allowedActions}
      />
    );
  }
  if (type === "UserTask_RevisePO") {
    return <RevisePurchaseOrderPage purchaseOrderId={purchaseOrderId!} taskKey={task.taskKey} />;
  }
  if (type === "UserTask_SupplierConfirmation") {
    return (
      <SupplierConfirmationPage
        purchaseOrderId={purchaseOrderId!}
        taskKey={task.taskKey}
        allowedActions={task.allowedActions}
      />
    );
  }
  if (type === "UserTask_TrackShipment") {
    return <TrackShipmentPage purchaseOrderId={purchaseOrderId!} taskKey={task.taskKey} />;
  }
  if (type === "UserTask_GoodsReceipt") {
    return <GoodsReceiptPage purchaseOrderId={purchaseOrderId!} taskKey={task.taskKey} />;
  }

  return (
    <>
      <PageHeader title={task.name} />
      <ErrorBanner message={`Unsupported task type: ${type}`} />
    </>
  );
}
