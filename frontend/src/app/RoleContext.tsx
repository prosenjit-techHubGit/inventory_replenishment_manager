import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { DEMO_ROLES, loadRoleId, saveRoleId, type DemoRole, type RoleId } from "./roles";

interface RoleContextValue {
  role: DemoRole;
  setRoleId: (id: RoleId) => void;
}

const RoleContext = createContext<RoleContextValue | null>(null);

export function RoleProvider({ children }: { children: ReactNode }) {
  const [roleId, setRoleIdState] = useState<RoleId>(loadRoleId);
  const role = useMemo(
    () => DEMO_ROLES.find((item) => item.id === roleId) ?? DEMO_ROLES[0],
    [roleId],
  );
  const setRoleId = (id: RoleId) => {
    setRoleIdState(id);
    saveRoleId(id);
  };
  return <RoleContext.Provider value={{ role, setRoleId }}>{children}</RoleContext.Provider>;
}

export function useRole() {
  const value = useContext(RoleContext);
  if (!value) {
    throw new Error("useRole must be used within RoleProvider");
  }
  return value;
}
