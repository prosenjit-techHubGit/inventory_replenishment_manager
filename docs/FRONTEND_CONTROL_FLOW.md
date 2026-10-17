# Frontend control flow

How React **app**, **features**, **components**, and **APIs** are wired, and how control passes from one to another.

The browser never talks to Camunda. Feature screens call `src/api/*`, which call `httpClient`, which `fetch`es `/api/v1`. Vite proxies that to Spring Boot.

---

## Scenario

**Marcus (procurement buyer) creates a purchase order. After Camunda opens an approval task, Elena (procurement manager) claims it in Inbox and approves it.**

Control always moves the same way:

**route → feature page → `useLoad` or a submit handler → `src/api/*` → `httpClient` → `/api/v1` (Vite proxy) → Spring**

Shared chrome (`AppShell`, demo role, toast) stays mounted while the page in `Outlet` changes.

---

## How the React tree is wired

```mermaid
flowchart TB
  main["main.tsx"] --> App
  App --> RoleProvider
  RoleProvider --> BrowserRouter
  BrowserRouter --> ToastProvider
  ToastProvider --> AppRouter
  AppRouter --> AppShell
  AppShell --> Sidebar["Sidebar + Acting as"]
  AppShell --> Outlet["Outlet: current page"]

  Outlet --> Dash["features/dashboard"]
  Outlet --> Tasks["features/tasks"]
  Outlet --> POs["features/purchase-orders"]
  Outlet --> Recs["features/recommendations"]
  Tasks --> TaskPage["TaskPage dispatcher"]
  TaskPage --> ReviewPO["ReviewPurchaseOrderPage"]
  TaskPage --> Supplier["SupplierConfirmationPage"]
  TaskPage --> GRN["GoodsReceipt / TrackShipment"]
  TaskPage --> ReviewRec["ReviewRecommendationPage"]

  Dash --> APIs["src/api/*"]
  POs --> APIs
  Tasks --> APIs
  Recs --> APIs
  ReviewPO --> APIs
  AppShell --> APIs
  APIs --> HTTP["httpClient.request"]
  HTTP --> Proxy["fetch /api/v1"]
  Proxy --> Spring["Spring Boot"]
```

`AppShell` never unmounts. Only the feature page inside `Outlet` swaps. `TaskPage` does not render a form itself: it loads the task from the backend, then picks a feature screen from `task.taskType`.

---

## Control flow for the scenario

```mermaid
sequenceDiagram
  actor User
  participant Role as RoleProvider
  participant Router as AppRouter
  participant Shell as AppShell
  participant Page as Feature page
  participant API as src/api
  participant HTTP as httpClient
  participant BE as Spring /api/v1

  User->>Router: open /
  Router->>Page: DashboardPage
  Page->>API: listInventory, listPurchaseOrders, listTasks(role.id)
  API->>HTTP: GET /api/v1/...
  HTTP->>BE: Vite proxy
  BE-->>Page: catalog + POs + inbox
  Shell->>API: listTasks(role.id)
  Note over Shell: inbox badge

  User->>Router: Create purchase order
  Router->>Page: CreatePurchaseOrderPage
  Page->>API: listInventory + listSuppliers
  User->>Page: submit form
  Page->>API: createPurchaseOrder + Idempotency-Key
  API->>BE: POST /api/v1/purchase-orders
  BE-->>Page: PO 202
  Page->>Router: navigate /purchase-orders/:id
  Router->>Page: PurchaseOrderDetailsPage
  Page->>API: getPurchaseOrder(id)

  User->>Role: Acting as Elena
  Role-->>Shell: role.id = PROCUREMENT_MANAGER
  Shell->>API: listTasks(PROCUREMENT_MANAGER)
  User->>Router: Inbox
  Router->>Page: TaskListPage
  Page->>API: listTasks(PROCUREMENT_MANAGER)
  User->>Page: Open / Claim
  Page->>Router: /tasks/:taskKey
  Router->>Page: TaskPage
  Page->>API: getTask + claimTask
  Page->>Page: taskType = UserTask_ProcurementApproval
  Page->>Page: render ReviewPurchaseOrderPage
  Page->>API: getPurchaseOrder
  User->>Page: Approve
  Page->>API: completeTask APPROVE
  API->>BE: POST /api/v1/tasks/{key}/complete
  Page->>Router: navigate /tasks
```

After Approve, Spring completes the Camunda user task. The next human step (finance, supply chain, supplier, warehouse) appears later in Inbox for that candidate group. The UI only reacts to whatever `GET /api/v1/tasks/{taskKey}` returns.

---

## Routes

| URL | Feature page |
|---|---|
| `/` | `DashboardPage` |
| `/tasks` | `TaskListPage` |
| `/tasks/:taskKey` | `TaskPage` (dispatcher) |
| `/purchase-orders` | `PurchaseOrderListPage` |
| `/purchase-orders/new` | `CreatePurchaseOrderPage` |
| `/purchase-orders/:id` | `PurchaseOrderDetailsPage` |
| `/recommendations` | `RecommendationsListPage` |
| `/recommendations/:id` | `RecommendationDetailPage` |

### TaskPage dispatcher

| `task.taskType` | Renders |
|---|---|
| `UserTask_ReviewRecommendation` | `ReviewRecommendationPage` |
| `UserTask_ProcurementApproval` | `ReviewPurchaseOrderPage` |
| `UserTask_FinanceApproval` | `ReviewPurchaseOrderPage` |
| `UserTask_SupplyChainApproval` | `ReviewPurchaseOrderPage` |
| `UserTask_RevisePO` | `RevisePurchaseOrderPage` |
| `UserTask_SupplierConfirmation` | `SupplierConfirmationPage` |
| `UserTask_TrackShipment` | `TrackShipmentPage` |
| `UserTask_GoodsReceipt` | `GoodsReceiptPage` |

---

## Folder roles

| Layer | Folder / file | Job |
|---|---|---|
| Boot | `src/main.tsx`, `src/app/App.tsx` | Mount React, wrap providers |
| Routing | `src/app/router.tsx` | URL → feature page |
| Chrome | `src/components/AppShell.tsx` | Nav, role switcher, inbox count |
| Features | `src/features/*` | Screens and submit handlers |
| Shared UI | `src/components/*` | `PageHeader`, badges, form fields |
| API | `src/api/*` | One module per backend resource |
| HTTP | `src/api/httpClient.ts` | `fetch`, JSON, 8s timeout, errors |
| Shared state | `RoleContext`, `ToastContext`, `useLoad` | Demo role, success toast, load/error |

The sidebar **Acting as** control is demo-only. It sets `candidateGroup` on `GET /api/v1/tasks`. It is not sent to Camunda from the browser.
