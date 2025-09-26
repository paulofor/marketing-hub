import { Route, Routes } from "react-router-dom";

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
import EditSuccessProductPage from "./pages/successProduct/EditSuccessProductPage";
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
import EditExperimentPage from "./pages/experiment/EditExperimentPage";
import NicheDetailPage from "./pages/niche/NicheDetailPage";
import HypothesisDetailPage from "./pages/hypothesis/HypothesisDetailPage";
import HypothesesPage from "./pages/hypothesis/HypothesesPage";
import HypothesisListPage from "./pages/hypothesis/HypothesisListPage";
import NewHypothesisPage from "./pages/hypothesis/NewHypothesisPage";
import EditHypothesisPage from "./pages/hypothesis/EditHypothesisPage";
import AppLayout from "./app/AppLayout";
import AnglesPage from "./pages/AnglesPage";
import VisualProofsPage from "./pages/VisualProofsPage";
import EmotionalTriggersPage from "./pages/EmotionalTriggersPage";
import LandingPreview from "./pages/landing/LandingPreview";
import AnalyticsDashboard from "./pages/landing/AnalyticsDashboard";
import FunnelListPage from "./pages/funnel/FunnelListPage";
import NewFunnelPage from "./pages/funnel/NewFunnelPage";
import EditFunnelPage from "./pages/funnel/EditFunnelPage";
import ChatDialogListPage from "./pages/chatDialog/ChatDialogListPage";
import NewChatDialogPage from "./pages/chatDialog/NewChatDialogPage";
import PromptEntitiesPage from "./pages/prompt/PromptEntitiesPage";
import PromptAttributesPage from "./pages/prompt/PromptAttributesPage";
import NewPromptEntityPage from "./pages/prompt/NewPromptEntityPage";
import PromptEntityDescriptionPage from "./pages/prompt/PromptEntityDescriptionPage";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import FacebookCampaignExperimentsPage from "./pages/facebook/FacebookCampaignExperimentsPage";
import FacebookExperimentsReadyPage from "./pages/facebook/FacebookExperimentsReadyPage";
import MainNavigation from "./components/MainNavigation";

export default function App() {
  return (
    <div className="container py-4">
      <MainNavigation />
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
        <Route
          path="/success-products/:id/edit"
          element={<EditSuccessProductPage />}
        />
        <Route path="/niches" element={<AppLayout />}>
          <Route index element={<NicheListPage />} />
          <Route path="new" element={<NewNichePage />} />
          <Route path=":nicheId" element={<NicheDetailPage />} />
          <Route path=":nicheId/edit" element={<EditNichePage />} />
          <Route
            path=":nicheId/hypotheses/new"
            element={<NewHypothesisPage />}
          />
          <Route
            path=":nicheId/hypotheses/:hypothesisId"
            element={<HypothesisDetailPage />}
          />
          <Route
            path=":nicheId/hypotheses/:hypothesisId/edit"
            element={<EditHypothesisPage />}
          />
        </Route>
        <Route path="/experiments" element={<ExperimentListPage />} />
        <Route path="/experiments/new" element={<NewExperimentPage />} />
        <Route path="/experiments/:id" element={<AppLayout />}>
          <Route index element={<ExperimentDetailPage />} />
          <Route path="edit" element={<EditExperimentPage />} />
        </Route>
        <Route path="/hypotheses" element={<HypothesisListPage />} />
        <Route path="/hypotheses/board" element={<HypothesesPage />} />
        <Route path="/ai-services" element={<AiServiceListPage />} />
        <Route path="/ai-services/new" element={<NewAiServicePage />} />
        <Route path="/ai-services/:id/edit" element={<EditAiServicePage />} />
        <Route path="/angles" element={<AnglesPage />} />
        <Route path="/visual-proofs" element={<VisualProofsPage />} />
        <Route path="/emotional-triggers" element={<EmotionalTriggersPage />} />
        <Route path="/landing/:id" element={<LandingPreview />} />
        <Route path="/analytics" element={<AnalyticsDashboard />} />
        <Route path="/funnels" element={<FunnelListPage />} />
        <Route path="/funnels/new" element={<NewFunnelPage />} />
        <Route path="/funnels/:id/edit" element={<EditFunnelPage />} />
        <Route path="/chat-dialogs" element={<ChatDialogListPage />} />
        <Route path="/chat-dialogs/new" element={<NewChatDialogPage />} />
        <Route path="/prompt-entities" element={<PromptEntitiesPage />} />
        <Route path="/prompt-entities/new" element={<NewPromptEntityPage />} />
        <Route
          path="/prompt-entities/:entityId"
          element={<PromptEntityDescriptionPage />}
        />
        <Route
          path="/prompt-entities/:entityId/attributes"
          element={<PromptAttributesPage />}
        />
        <Route path="/facebook-campaigns" element={<FacebookCampaignExperimentsPage />} />
        <Route
          path="/facebook-campaigns/ready"
          element={<FacebookExperimentsReadyPage />}
        />
        <Route path="*" element={<div>Início</div>} />
      </Routes>
      <ToastContainer position="top-right" />
    </div>
  );
}
