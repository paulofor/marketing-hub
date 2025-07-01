import { Route, Routes, Link } from "react-router-dom";
import FacebookAccountsPage from "./pages/FacebookAccountsPage";
import InstagramAccountsPage from "./pages/InstagramAccountsPage";

export default function App() {
  return (
    <div className="p-4">
      <nav>
        <Link to="/accounts/facebook">Facebook Accounts</Link> |{" "}
        <Link to="/accounts/instagram">Instagram Accounts</Link>
      </nav>
      <Routes>
        <Route path="/accounts/facebook" element={<FacebookAccountsPage />} />
        <Route path="/accounts/instagram" element={<InstagramAccountsPage />} />
        <Route path="*" element={<div>Home</div>} />
      </Routes>
    </div>
  );
}
