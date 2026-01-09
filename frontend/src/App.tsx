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
import AppIdeaListPage from "./pages/appIdea/AppIdeaListPage";
import NewAppIdeaPage from "./pages/appIdea/NewAppIdeaPage";
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
import OpenAiModelListPage from "./pages/openaiModel/OpenAiModelListPage";
import NewOpenAiModelPage from "./pages/openaiModel/NewOpenAiModelPage";
import EditOpenAiModelPage from "./pages/openaiModel/EditOpenAiModelPage";
import MicroserviceListPage from "./pages/microservice/MicroserviceListPage";
import NewMicroservicePage from "./pages/microservice/NewMicroservicePage";
import EditMicroservicePage from "./pages/microservice/EditMicroservicePage";
import DifferentiatedTechnologyListPage from "./pages/differentiatedTechnology/DifferentiatedTechnologyListPage";
import NewDifferentiatedTechnologyPage from "./pages/differentiatedTechnology/NewDifferentiatedTechnologyPage";
import EditDifferentiatedTechnologyPage from "./pages/differentiatedTechnology/EditDifferentiatedTechnologyPage";
import MicroserviceExceptionListPage from "./pages/microservice/MicroserviceExceptionListPage";
import ExperimentListPage from "./pages/experiment/ExperimentListPage";
import NewExperimentPage from "./pages/experiment/NewExperimentPage";
import ExperimentDetailPage from "./pages/experiment/ExperimentDetailPage";
import EditExperimentPage from "./pages/experiment/EditExperimentPage";
import InstantFormDetailPage from "./pages/experiment/InstantFormDetailPage";
import ExperimentEmailDetailPage from "./pages/experiment/ExperimentEmailDetailPage";
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
import JourneyListPage from "./pages/journey/JourneyListPage";
import JourneyDetailPage from "./pages/journey/JourneyDetailPage";
import EditJourneyPage from "./pages/journey/EditJourneyPage";
import JourneyTemplatesPage from "./pages/journey/JourneyTemplatesPage";
import JourneyTemplateDetailPage from "./pages/journey/JourneyTemplateDetailPage";
import NewJourneyTemplatePage from "./pages/journey/NewJourneyTemplatePage";
import EditJourneyTemplatePage from "./pages/journey/EditJourneyTemplatePage";
import InteractionJourneyListPage from "./pages/interactionJourney/InteractionJourneyListPage";
import NewInteractionJourneyPage from "./pages/interactionJourney/NewInteractionJourneyPage";
import EditInteractionJourneyPage from "./pages/interactionJourney/EditInteractionJourneyPage";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import MainNavigation from "./components/MainNavigation";
import "./App.css";
import FacebookCampaignExperimentsPage from "./pages/facebook/FacebookCampaignExperimentsPage";
import FacebookExperimentsReadyPage from "./pages/facebook/FacebookExperimentsReadyPage";
import GlobalAutomationAlerts from "./components/GlobalAutomationAlerts";
import AiGenerationListPage from "./pages/ai/AiGenerationListPage";
import AiWorkerPendingRequestsPage from "./pages/ai/AiWorkerPendingRequestsPage";
import PrivacyPolicySettingsPage from "./pages/settings/PrivacyPolicySettingsPage";
import WhatsAppConsolePage from "./pages/whatsapp/WhatsAppConsolePage";
import LeadPortalExperimentMetricsPage from "./pages/leadPortal/LeadPortalExperimentMetricsPage";
import LeadPortalImagesPage from "./pages/leadPortal/LeadPortalImagesPage";
import LeadPortalImagePackageDetailPage from "./pages/leadPortal/LeadPortalImagePackageDetailPage";
import PaymentsDashboardPage from "./pages/payments/PaymentsDashboardPage";
import PaymentDetailPage from "./pages/payments/PaymentDetailPage";

export default function App() {
  return (
    <div className="app-shell">
      <MainNavigation />
      <div className="app-shell__content">
        <main className="app-shell__main">
          <div className="container-fluid py-4">
            <GlobalAutomationAlerts />
            <Routes>
              <Route
                path="/accounts/facebook"
                element={<FacebookAccountsPage />}
              />
              <Route
                path="/accounts/instagram"
                element={<InstagramAccountsPage />}
              />
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
              <Route path="/app-ideas" element={<AppIdeaListPage />} />
              <Route path="/app-ideas/new" element={<NewAppIdeaPage />} />
              <Route path="/products" element={<ProductListPage />} />
              <Route path="/products/new" element={<NewProductPage />} />
              <Route
                path="/success-products"
                element={<SuccessProductListPage />}
              />
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
                <Route path="instant-forms/:instantFormId" element={<InstantFormDetailPage />} />
                <Route path="emails/:emailStepId" element={<ExperimentEmailDetailPage />} />
              </Route>
              <Route path="/hypotheses" element={<HypothesisListPage />} />
              <Route path="/hypotheses/board" element={<HypothesesPage />} />
              <Route path="/ai-services" element={<AiServiceListPage />} />
              <Route path="/ai-services/new" element={<NewAiServicePage />} />
              <Route
                path="/ai-services/:id/edit"
                element={<EditAiServicePage />}
              />
              <Route path="/openai-models" element={<OpenAiModelListPage />} />
              <Route path="/openai-models/new" element={<NewOpenAiModelPage />} />
              <Route
                path="/openai-models/:id/edit"
                element={<EditOpenAiModelPage />}
              />
              <Route
                path="/microservices"
                element={<MicroserviceListPage />}
              />
              <Route
                path="/microservices/errors"
                element={<MicroserviceExceptionListPage />}
              />
              <Route
                path="/microservices/new"
                element={<NewMicroservicePage />}
              />
              <Route
                path="/microservices/:id/edit"
                element={<EditMicroservicePage />}
              />
              <Route
                path="/differentiated-technologies"
                element={<DifferentiatedTechnologyListPage />}
              />
              <Route
                path="/differentiated-technologies/new"
                element={<NewDifferentiatedTechnologyPage />}
              />
              <Route
                path="/differentiated-technologies/:id/edit"
                element={<EditDifferentiatedTechnologyPage />}
              />
              <Route
                path="/ai/generations"
                element={<AiGenerationListPage />}
              />
              <Route
                path="/ai/pending-requests"
                element={<AiWorkerPendingRequestsPage />}
              />
              <Route path="/angles" element={<AnglesPage />} />
              <Route path="/visual-proofs" element={<VisualProofsPage />} />
              <Route
                path="/emotional-triggers"
                element={<EmotionalTriggersPage />}
              />
              <Route path="/landing/:id" element={<LandingPreview />} />
              <Route path="/analytics" element={<AnalyticsDashboard />} />
              <Route path="/funnels" element={<FunnelListPage />} />
              <Route path="/funnels/new" element={<NewFunnelPage />} />
              <Route
                path="/funnels/:id/edit"
                element={<EditFunnelPage />}
              />
              <Route
                path="/lead-portal/metrics"
                element={<LeadPortalExperimentMetricsPage />}
              />
              <Route
                path="/lead-portal/images"
                element={<LeadPortalImagesPage />}
              />
              <Route
                path="/lead-portal/images/:packageId"
                element={<LeadPortalImagePackageDetailPage />}
              />
              <Route path="/payments" element={<PaymentsDashboardPage />} />
              <Route path="/payments/:id" element={<PaymentDetailPage />} />
              <Route path="/chat-dialogs" element={<ChatDialogListPage />} />
              <Route path="/chat-dialogs/new" element={<NewChatDialogPage />} />
              <Route path="/prompt-entities" element={<PromptEntitiesPage />} />
              <Route
                path="/prompt-entities/new"
                element={<NewPromptEntityPage />}
              />
              <Route
                path="/prompt-entities/:entityId"
                element={<PromptEntityDescriptionPage />}
              />
              <Route
                path="/prompt-entities/:entityId/attributes"
                element={<PromptAttributesPage />}
              />
              <Route
                path="/facebook-campaigns"
                element={<FacebookCampaignExperimentsPage />}
              />
              <Route
                path="/facebook-campaigns/ready"
                element={<FacebookExperimentsReadyPage />}
              />
              <Route path="/journeys" element={<JourneyListPage />} />
              <Route path="/journeys/:id" element={<JourneyDetailPage />} />
              <Route
                path="/journeys/:id/edit"
                element={<EditJourneyPage />}
              />
              <Route
                path="/journey-templates"
                element={<JourneyTemplatesPage />}
              />
              <Route
                path="/journey-templates/:id"
                element={<JourneyTemplateDetailPage />}
              />
              <Route
                path="/journey-templates/:id/edit"
                element={<EditJourneyTemplatePage />}
              />
              <Route
                path="/journey-templates/new"
                element={<NewJourneyTemplatePage />}
              />
              <Route
                path="/interaction-journeys"
                element={<InteractionJourneyListPage />}
              />
              <Route
                path="/interaction-journeys/new"
                element={<NewInteractionJourneyPage />}
              />
              <Route
                path="/interaction-journeys/:id/edit"
                element={<EditInteractionJourneyPage />}
              />
              <Route path="/whatsapp" element={<WhatsAppConsolePage />} />
              <Route
                path="/settings/privacy-policy"
                element={<PrivacyPolicySettingsPage />}
              />
              <Route path="*" element={<div>Início</div>} />
            </Routes>
          </div>
        </main>
        <ToastContainer position="top-right" />
      </div>
    </div>
  );
}
