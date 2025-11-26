import { BrowserRouter, Route, Routes } from "react-router-dom";
import FlowPage from "./pages/FlowPage";
import HomePage from "./pages/HomePage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/flows/:slug" element={<FlowPage />} />
      </Routes>
    </BrowserRouter>
  );
}
