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
        <Route path="*" element={<div>Início</div>} />
      </Routes>
    </div>
  );
}
