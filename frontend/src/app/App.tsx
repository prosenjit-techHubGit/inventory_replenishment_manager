import { BrowserRouter } from "react-router-dom";
import { RoleProvider } from "./RoleContext";
import { ToastProvider } from "./ToastContext";
import { AppRouter } from "./router";

export function App() {
  return (
    <RoleProvider>
      <BrowserRouter>
        <ToastProvider>
          <AppRouter />
        </ToastProvider>
      </BrowserRouter>
    </RoleProvider>
  );
}
