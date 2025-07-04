import { Route, Routes, Link } from "react-router-dom";

import FacebookAccountsPage from "./pages/FacebookAccountsPage";
import InstagramAccountsPage from "./pages/InstagramAccountsPage";

export default function App() {
  return (
    <div className="container py-4">
      <nav className="navbar navbar-expand-lg navbar-light bg-light mb-4">
        <div className="container-fluid">
          <Link className="navbar-brand" to="/">
            Marketing Hub
          </Link>
          <div className="navbar-nav">
            <Link className="nav-link" to="/accounts/facebook">
              Facebook Accounts
            </Link>
            <Link className="nav-link" to="/accounts/instagram">
              Instagram Accounts
            </Link>
          </div>
        </div>
      </nav>
      <Routes>
        <Route path="/accounts/facebook" element={<FacebookAccountsPage />} />
        <Route path="/accounts/instagram" element={<InstagramAccountsPage />} />
        <Route path="*" element={<div>Home</div>} />
      </Routes>
    </div>
  );
}
