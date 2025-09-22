import { Outlet } from "react-router-dom";
import BreadcrumbsProvider from "./breadcrumbs";

export default function AppLayout() {
  return (
    <BreadcrumbsProvider>
      <Outlet />
    </BreadcrumbsProvider>
  );
}
