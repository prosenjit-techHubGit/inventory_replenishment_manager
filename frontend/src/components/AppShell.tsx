import { NavLink, Outlet } from "react-router-dom";
import { useLoad } from "../app/useLoad";
import { useRole } from "../app/RoleContext";
import { DEMO_ROLES, type RoleId } from "../app/roles";
import { listTasks } from "../api/tasksApi";
import { LtmLogo } from "./LtmLogo";

export function AppShell() {
  const { role, setRoleId } = useRole();
  const { data: tasks } = useLoad(() => listTasks(role.id).catch(() => []), [role.id]);
  const inboxCount = tasks?.length ?? 0;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <LtmLogo />
          <span className="sidebar-caption">Inventory replenishment</span>
        </div>
        <nav className="sidebar-nav">
          <NavLink to="/" end className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
            Dashboard
          </NavLink>
          <NavLink to="/tasks" className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}>
            Inbox
            {inboxCount > 0 ? <span className="nav-badge">{inboxCount}</span> : null}
          </NavLink>
          <NavLink
            to="/purchase-orders"
            className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}
          >
            Purchase orders
          </NavLink>
          <NavLink
            to="/recommendations"
            className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}
          >
            Recommendations
          </NavLink>
        </nav>
        <div className="role-switcher">
          <label htmlFor="acting-role">Acting as</label>
          <select
            id="acting-role"
            value={role.id}
            onChange={(event) => setRoleId(event.target.value as RoleId)}
          >
            {DEMO_ROLES.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
          <div className="role-meta">{role.title}</div>
        </div>
        <div className="sidebar-foot">A Larsen &amp; Toubro Group Company</div>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
