import { BrowserRouter } from "react-router-dom";
import { RoleProvider } from "./RoleContext";
import { AppRouter } from "./router";

export function App() {
  return (
    <RoleProvider>
      <BrowserRouter>
        <AppRouter />
      </BrowserRouter>
    </RoleProvider>
  );
}
