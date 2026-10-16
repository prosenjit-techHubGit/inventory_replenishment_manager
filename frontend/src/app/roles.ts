export type RoleId =
  | "INVENTORY_PLANNER"
  | "PROCUREMENT_BUYER"
  | "PROCUREMENT_MANAGER"
  | "FINANCE_APPROVER"
  | "SUPPLY_CHAIN_MANAGER"
  | "SUPPLIER_USER"
  | "WAREHOUSE_RECEIVER";

export interface DemoRole {
  id: RoleId;
  name: string;
  title: string;
}

export const DEMO_ROLES: DemoRole[] = [
  { id: "INVENTORY_PLANNER", name: "Priya Chen", title: "Inventory planner" },
  { id: "PROCUREMENT_BUYER", name: "Marcus Hale", title: "Procurement buyer" },
  { id: "PROCUREMENT_MANAGER", name: "Elena Voss", title: "Procurement manager" },
  { id: "FINANCE_APPROVER", name: "David Okonkwo", title: "Finance approver" },
  { id: "SUPPLY_CHAIN_MANAGER", name: "Mei Tan", title: "Supply chain manager" },
  { id: "SUPPLIER_USER", name: "Kenji Mori", title: "Supplier" },
  { id: "WAREHOUSE_RECEIVER", name: "Ana Ruiz", title: "Warehouse receiver" },
];

const STORAGE_KEY = "ltm.actingRole";

export function loadRoleId(): RoleId {
  const stored = localStorage.getItem(STORAGE_KEY);
  return DEMO_ROLES.some((role) => role.id === stored) ? (stored as RoleId) : "INVENTORY_PLANNER";
}

export function saveRoleId(roleId: RoleId): void {
  localStorage.setItem(STORAGE_KEY, roleId);
}
