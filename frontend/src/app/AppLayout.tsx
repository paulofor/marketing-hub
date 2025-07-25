import { Link, Outlet } from "react-router-dom";
import BreadcrumbsProvider from "./breadcrumbs";

export default function AppLayout() {
  return (
    <BreadcrumbsProvider>
      <header className="mb-3">
        <Link to="/niches" className="navbar-brand">
          Marketing Hub
        </Link>
      </header>
      <Outlet />
    </BreadcrumbsProvider>
  );
}
