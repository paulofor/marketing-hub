import { Route, Routes, Link } from 'react-router-dom';
import AccountsPage from './pages/AccountsPage';

export default function App() {
  return (
    <div className="p-4">
      <nav>
        <Link to="/accounts">Facebook Accounts</Link>
      </nav>
      <Routes>
        <Route path="/accounts" element={<AccountsPage />} />
        <Route path="*" element={<div>Home</div>} />
      </Routes>
    </div>
  );
}
