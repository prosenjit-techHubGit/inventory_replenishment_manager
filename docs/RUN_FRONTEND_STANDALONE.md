# Run the frontend on Vite against another backend

This app’s UI lives entirely under `frontend/`. It is a standalone Vite + React + TypeScript SPA. The browser never talks to Camunda; it only calls a REST API under `/api/v1`. Any backend that implements that contract can replace the Spring Boot service in this repo.

These steps extract that UI, run it with Vite, and point it at a different backend.

---

## 1. What you are extracting

Copy the `frontend` directory only. You do **not** need `backend/`, `camunda/`, Maven, Java, or PostgreSQL on the machine that serves the UI.

Required contents:

```text
frontend/
  index.html
  package.json
  package-lock.json
  tsconfig.json
  vite.config.ts
  public/ltm-logo.svg
  src/                  # React app
```

Do not copy `frontend/node_modules/` or `frontend/dist/`. Those are generated.

---

## 2. Prerequisites

- **Node.js 20 or later** (Vite 8)
- npm (comes with Node)
- Network reachability to the other backend (hostname, port, TLS as applicable)

Check Node:

```bash
node -v
npm -v
```

---

## 3. Copy the frontend to its own folder

From the repo root.

**PowerShell**

```powershell
Copy-Item -Recurse frontend C:\path\to\procurement-ui
cd C:\path\to\procurement-ui
```

**bash**

```bash
cp -R frontend /path/to/procurement-ui
cd /path/to/procurement-ui
```

You can also clone this repo and work only in `frontend/` without copying.

---

## 4. Install dependencies and start Vite

```bash
npm install
npm run dev
```

Vite serves the UI at **http://localhost:5173**.

By default `vite.config.ts` proxies every browser call that starts with `/api` to `http://localhost:8080`. If nothing is listening there, the UI shows “Unable to reach the API.”

---

## 5. Point Vite at the other backend (local / same machine)

This is the usual way during development. The browser still calls `/api/v1/...` on port 5173; Vite forwards those requests to the other backend. The browser origin stays `http://localhost:5173`, so you often **do not** need CORS on the backend.

1. Open `vite.config.ts`.
2. Change the proxy `target` to the other backend’s origin (scheme + host + port, no path).

```ts
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://HOST:PORT", // e.g. http://127.0.0.1:8081 or https://api.example.com
        changeOrigin: true,
        secure: true, // set false only if the backend TLS cert is untrusted
      },
    },
  },
});
```

3. Stop Vite (`Ctrl+C`) and start it again:

```bash
npm run dev
```

Vite reads `vite.config.ts` at startup. Changing the target while it is running is not enough.

**Check the proxy**

Open http://localhost:5173 and use DevTools → Network. A successful inventory load looks like:

- Request URL: `http://localhost:5173/api/v1/inventory`
- It is proxied to: `http://HOST:PORT/api/v1/inventory`

The other backend must expose `/api/v1/...` at its root (no extra servlet context path), or you must add a Vite `rewrite` (see Troubleshooting).

---

## 6. Call a remote backend directly from the browser (optional)

Use this when you cannot proxy (for example the UI is already built and hosted, or the backend is on another origin and you want the browser to call it directly).

The fetch helper in `src/api/httpClient.ts` uses relative paths such as `/api/v1/inventory`. Relative paths always hit the Vite origin, not a remote host. Add a base URL.

### 6.1 Add an env file

Create `frontend/.env.local` (do not commit secrets; this file is for local overrides):

```env
VITE_API_BASE_URL=https://other-backend.example.com
```

No trailing slash. Leave it empty or omit it to keep using the Vite proxy.

### 6.2 Prefix every API call

In `src/api/httpClient.ts`, change `fetch(path, ...)` to use the env base:

```ts
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  // ... existing header / timeout setup ...
  response = await fetch(`${API_BASE}${path}`, { ...init, headers, signal: controller.signal });
```

Restart Vite after creating or changing `.env.local`.

### 6.3 CORS on the other backend

Direct browser calls are cross-origin. The backend must allow:

- Origin: `http://localhost:5173` (and the production UI origin when you deploy)
- Methods: `GET`, `POST` (and `PUT` / `DELETE` if you add them)
- Headers: `Content-Type`, `Idempotency-Key`

This repo’s Spring Boot app does that with:

```text
FRONTEND_ORIGIN=http://localhost:5173
```

(`app.cors.allowed-origins` in `application.yml`). If the other backend is not this Spring app, configure equivalent CORS there.

---

## 7. What the other backend must implement

The UI assumes REST JSON under **`/api/v1`**. Field names are camelCase. Dates that are calendar days are `YYYY-MM-DD`. Task keys are strings (they may be large integers).

### Catalog

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/inventory` | Array of items |
| GET | `/api/v1/suppliers` | Array of suppliers |

Inventory item: `id`, `sku`, `name`, `onHandQuantity`, `reorderLevel`, `openPurchaseQuantity`, `projectedAvailable`, `criticalItem`, `replenishmentRequired`.

Supplier: `id`, `code`, `name`, `active`, `unitPrice`, `leadTimeDays`, `deliveryScore`, `qualityScore`, `riskLevel` (`LOW` \| `MEDIUM` \| `HIGH`).

### Purchase orders

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/purchase-orders` | Array of PO summaries |
| GET | `/api/v1/purchase-orders/{id}` | Detail wrapper (not a flat PO) |
| POST | `/api/v1/purchase-orders` | Header `Idempotency-Key: <uuid>` required |

Create body:

```json
{
  "itemId": "<uuid>",
  "quantity": 50,
  "supplierId": "<uuid>",
  "unitPrice": 125.5,
  "requiredDeliveryDate": "2026-11-15",
  "urgency": "NORMAL",
  "businessJustification": "..."
}
```

`urgency` is `NORMAL` \| `HIGH` \| `URGENT`. Same key + same body must return the same PO (this UI retries create). Typical success status is **202**.

Detail payload shape:

```json
{
  "purchaseOrder": { },
  "itemSku": "...",
  "itemName": "...",
  "criticalItem": true,
  "supplierCode": "...",
  "supplierName": "...",
  "shipments": [],
  "receipts": [],
  "approvals": [],
  "cumulativeAcceptedQuantity": 0,
  "remainingQuantity": 50
}
```

PO summary fields used by the UI include `purchaseOrderId`, `poNumber`, `poRevision`, `status`, `initiationType`, `poRequestId`, `itemId`, `supplierId`, `recommendationId`, `quantity`, `unitPrice`, `totalValue`, `requiredDeliveryDate`, `urgency`, `supplierRisk`, `businessJustification`, `createdAt`.

### Recommendations

| Method | Path |
|---|---|
| GET | `/api/v1/recommendations` |
| GET | `/api/v1/recommendations/{id}` |

### Tasks (required for inbox and workflow screens)

The UI does **not** call Camunda. The backend is the task façade.

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/tasks?candidateGroup=ROLE&activeOnly=true` | Inbox |
| GET | `/api/v1/tasks/{taskKey}` | Detail |
| POST | `/api/v1/tasks/{taskKey}/claim` | Empty body |
| POST | `/api/v1/tasks/{taskKey}/complete` | JSON body below |

Complete body:

```json
{
  "action": "APPROVE",
  "variables": { }
}
```

`taskType` / `elementId` values the UI routes on:

| elementId | Screen | Actions the UI sends |
|---|---|---|
| `UserTask_ReviewRecommendation` | Recommendation review | `ACCEPT`, `MODIFY_AND_ACCEPT`, `REJECT` |
| `UserTask_ProcurementApproval` | PO approval | `APPROVE`, `RETURN_FOR_REVISION`, `REJECT` |
| `UserTask_FinanceApproval` | PO approval | same |
| `UserTask_SupplyChainApproval` | PO approval | same |
| `UserTask_RevisePO` | Revise PO | `SUBMIT_REVISION` |
| `UserTask_SupplierConfirmation` | Supplier | `CONFIRMED`, `REVISED_DATE`, `REJECTED` |
| `UserTask_TrackShipment` | Delivery | `DELIVERED` |
| `UserTask_GoodsReceipt` | GRN | `RECORD_RECEIPT` |

Task detail must include `data.purchaseOrderId` and/or `data.poRequestId` (and `data.recommendationId` for recommendation tasks). Inbox rows use `taskKey`, `name`, `elementId`, `bpmnProcessId`, `assignee`, `businessId`, `creationDate`.

The sidebar “Acting as” control is **demo-only**. It sets `candidateGroup` on `GET /api/v1/tasks`. Groups:

`INVENTORY_PLANNER`, `PROCUREMENT_BUYER`, `PROCUREMENT_MANAGER`, `FINANCE_APPROVER`, `SUPPLY_CHAIN_MANAGER`, `SUPPLIER_USER`, `WAREHOUSE_RECEIVER`.

### Errors

Non-2xx JSON the UI reads:

```json
{ "code": "BUSINESS_VALIDATION_ERROR", "message": "Human readable text" }
```

`204` with an empty body is treated as success for claim/complete.

---

## 8. Confirm it is talking to the other backend

1. Open http://localhost:5173.
2. Dashboard should list inventory from that backend (not a blank “Unable to reach the API”).
3. DevTools → Network: `/api/v1/inventory` and `/api/v1/purchase-orders` return **200**.
4. Create purchase order (Procurement buyer / any role): submit should return **202** and then open PO detail.
5. Switch **Acting as** to a role that has work; Inbox should list that role’s tasks from the other backend.

---

## 9. Production build (optional)

```bash
npm run build
npm run preview
```

`npm run build` writes static files to `dist/`. `preview` serves them (default port 4173).

`vite preview` does **not** use the `server.proxy` from `vite.config.ts`. For a built UI you must either:

- put a reverse proxy in front (`/api` → other backend), or
- use `VITE_API_BASE_URL` from step 6 and rebuild.

Set the backend CORS origin to the preview/production UI origin (for example `http://localhost:4173`).

---

## 10. Troubleshooting

| Symptom | Likely cause | What to do |
|---|---|---|
| “Unable to reach the API” | Backend down, wrong proxy target, or CORS | Hit `http://HOST:PORT/api/v1/inventory` with curl. Fix `target` or CORS. |
| 404 on `/api/v1/...` | Backend has a context path (`/procurement/api/v1`) | Add `rewrite` in the Vite proxy, or strip the context path on the backend. |
| Browser CORS error | Direct `VITE_API_BASE_URL` without CORS | Allow origin `http://localhost:5173` and header `Idempotency-Key`. |
| Create PO hangs / 502 | Proxy target unreachable | Confirm the other backend is listening on the host/port in `vite.config.ts`. |
| Inbox empty | `candidateGroup` does not match the other backend’s groups | Align group names with the list in step 7. |
| Task screen “not linked to a purchase order” | Task detail missing `purchaseOrderId` / `poRequestId` | Return those in `data` on `GET /api/v1/tasks/{taskKey}`. |
| Env change has no effect | Vite already running | Restart `npm run dev`. |

**curl smoke test** (replace host):

```bash
curl http://HOST:PORT/api/v1/inventory
curl http://HOST:PORT/api/v1/suppliers
curl http://HOST:PORT/api/v1/purchase-orders
curl "http://HOST:PORT/api/v1/tasks?candidateGroup=PROCUREMENT_MANAGER&activeOnly=true"
```

---

## 11. Boundary you must keep

Do not point the Vite app at the Camunda REST API. Task search, claim, and complete go to **your** `/api/v1/tasks` façade. Camunda credentials stay on the backend.
