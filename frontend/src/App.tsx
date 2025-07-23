import { Route, Routes, Link } from "react-router-dom";

import FacebookAccountsPage from "./pages/FacebookAccountsPage";
import InstagramAccountsPage from "./pages/InstagramAccountsPage";
import MediaListPage from "./pages/media/MediaListPage";
import NewMediaPage from "./pages/media/NewMediaPage";
import MediaDetailPage from "./pages/media/MediaDetailPage";
import CoursePlanListPage from "./pages/course/CoursePlanListPage";
import NewCoursePlanPage from "./pages/course/NewCoursePlanPage";
import CoursePlanDetailPage from "./pages/course/CoursePlanDetailPage";
import ProductListPage from "./pages/product/ProductListPage";
import NewProductPage from "./pages/product/NewProductPage";
import SuccessProductListPage from "./pages/successProduct/SuccessProductListPage";
import NewSuccessProductPage from "./pages/successProduct/NewSuccessProductPage";
import SuccessProductDetailPage from "./pages/successProduct/SuccessProductDetailPage";
import InstagramPostsPage from "./pages/post/InstagramPostsPage";
import NicheListPage from "./pages/niche/NicheListPage";
import NewNichePage from "./pages/niche/NewNichePage";
import EditNichePage from "./pages/niche/EditNichePage";
import AiServiceListPage from "./pages/aiService/AiServiceListPage";
import NewAiServicePage from "./pages/aiService/NewAiServicePage";
import EditAiServicePage from "./pages/aiService/EditAiServicePage";
import ExperimentListPage from "./pages/experiment/ExperimentListPage";
import NewExperimentPage from "./pages/experiment/NewExperimentPage";
import ExperimentDetailPage from "./pages/experiment/ExperimentDetailPage";
import HypothesesPage from "./pages/hypothesis/HypothesesPage";
import AnglesPage from "./pages/AnglesPage";
import VisualProofsPage from "./pages/VisualProofsPage";
import EmotionalTriggersPage from "./pages/EmotionalTriggersPage";
import LandingPreview from "./pages/landing/LandingPreview";
import AnalyticsDashboard from "./pages/landing/AnalyticsDashboard";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

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
              Contas do Facebook
            </Link>
            <Link className="nav-link" to="/accounts/instagram">
              Contas do Instagram
            </Link>
            <Link className="nav-link" to="/media">
              Mídia
            </Link>
            <Link className="nav-link" to="/courses">
              Cursos
            </Link>
            <Link className="nav-link" to="/products">
              Produtos
            </Link>
            <Link className="nav-link" to="/success-products">
              Produtos de Sucesso
            </Link>
            <Link className="nav-link" to="/niches">
              Nichos
            </Link>
            <Link className="nav-link" to="/experiments">
              Testes de Nicho
            </Link>
            <Link className="nav-link" to="/hypotheses">
              Hipóteses
            </Link>
            <Link className="nav-link" to="/ai-services">
              IA
            </Link>
            <Link className="nav-link" to="/angles">
              Angles
            </Link>
            <Link className="nav-link" to="/visual-proofs">
              Provas Visuais
            </Link>
            <Link className="nav-link" to="/emotional-triggers">
              Gatilhos Emocionais
            </Link>
            <div className="nav-item dropdown">
              <span
                className="nav-link dropdown-toggle"
                id="market-test-dropdown"
                role="button"
                data-bs-toggle="dropdown"
                aria-expanded="false"
              >
                Teste de Mercado
              </span>
              <ul
                className="dropdown-menu"
                aria-labelledby="market-test-dropdown"
              >
                <li>
                  <a className="dropdown-item" href="#">
                    1- Hipotese e Oferta Isca
                  </a>
                </li>
                <li>
                  <a className="dropdown-item" href="#">
                    2- Funil Mínimo
                  </a>
                </li>
                <li>
                  <a className="dropdown-item" href="#">
                    3- Trafego e Segmentação
                  </a>
                </li>
                <li>
                  <a className="dropdown-item" href="#">
                    4- KPIs e limiares de decisão
                  </a>
                </li>
                <li>
                  <a className="dropdown-item" href="#">
                    5- Automação analítica
                  </a>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </nav>
      <Routes>
        <Route path="/accounts/facebook" element={<FacebookAccountsPage />} />
        <Route path="/accounts/instagram" element={<InstagramAccountsPage />} />
        <Route
          path="/accounts/instagram/:id/posts"
          element={<InstagramPostsPage />}
        />
        <Route path="/media" element={<MediaListPage />} />
        <Route path="/media/new" element={<NewMediaPage />} />
        <Route path="/media/:id" element={<MediaDetailPage />} />
        <Route path="/courses" element={<CoursePlanListPage />} />
        <Route path="/courses/new" element={<NewCoursePlanPage />} />
        <Route path="/courses/:id" element={<CoursePlanDetailPage />} />
        <Route path="/products" element={<ProductListPage />} />
        <Route path="/products/new" element={<NewProductPage />} />
        <Route path="/success-products" element={<SuccessProductListPage />} />
        <Route
          path="/success-products/new"
          element={<NewSuccessProductPage />}
        />
        <Route
          path="/success-products/:id"
          element={<SuccessProductDetailPage />}
        />
        <Route path="/niches" element={<NicheListPage />} />
        <Route path="/niches/new" element={<NewNichePage />} />
        <Route path="/niches/:id/edit" element={<EditNichePage />} />
        <Route path="/experiments" element={<ExperimentListPage />} />
        <Route path="/experiments/new" element={<NewExperimentPage />} />
        <Route path="/experiments/:id" element={<ExperimentDetailPage />} />
        <Route path="/hypotheses" element={<HypothesesPage />} />
        <Route path="/ai-services" element={<AiServiceListPage />} />
        <Route path="/ai-services/new" element={<NewAiServicePage />} />
        <Route path="/ai-services/:id/edit" element={<EditAiServicePage />} />
        <Route path="/angles" element={<AnglesPage />} />
        <Route path="/visual-proofs" element={<VisualProofsPage />} />
        <Route path="/emotional-triggers" element={<EmotionalTriggersPage />} />
        <Route path="/landing/:id" element={<LandingPreview />} />
        <Route path="/analytics" element={<AnalyticsDashboard />} />
        <Route path="*" element={<div>Início</div>} />
      </Routes>
      <ToastContainer position="top-right" />
    </div>
  );
}
