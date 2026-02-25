import { BrowserRouter, Route, Routes } from "react-router-dom";
import FlowPage from "./pages/FlowPage";
import HomePage from "./pages/HomePage";
import ImageDashboardPage from "./pages/ImageDashboardPage";
import ImageCasePage from "./pages/ImageCasePage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/flows/:slug" element={<FlowPage />} />
        <Route path="/monitoramento/imagens" element={<ImageDashboardPage />} />
        <Route path="/monitoramento/imagens/casos/:submissionId" element={<ImageCasePage />} />
      </Routes>
    </BrowserRouter>
  );
}
