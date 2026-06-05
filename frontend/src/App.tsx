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
import ProductSalesVideoPage from "./pages/salesVideo/ProductSalesVideoPage";
import SalesVideoProfileDetailPage from "./pages/salesVideo/SalesVideoProfileDetailPage";
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
import PipelineCrudPage from "./pages/pipeline/PipelineCrudPage";
import ExperimentListPage from "./pages/experiment/ExperimentListPage";
import NewExperimentPage from "./pages/experiment/NewExperimentPage";
import ExperimentDetailPage from "./pages/experiment/ExperimentDetailPage";
import EditExperimentPage from "./pages/experiment/EditExperimentPage";
import InstantFormDetailPage from "./pages/experiment/InstantFormDetailPage";
import ExperimentEmailDetailPage from "./pages/experiment/ExperimentEmailDetailPage";
import ExperimentAdSetWorkflowPage from "./pages/experiment/ExperimentAdSetWorkflowPage";
import ExperimentAdSetJobDetailPage from "./pages/experiment/ExperimentAdSetJobDetailPage";
import ExperimentFrameworkImageDetailsPage from "./pages/experiment/ExperimentFrameworkImageDetailsPage";
import ExperimentFacebookApiLogsPage from "./pages/experiment/ExperimentFacebookApiLogsPage";
import ExperimentPipelineJobsPage from "./pages/experiment/ExperimentPipelineJobsPage";
import ExperimentGeraLandingExecutionDetailPage from "./pages/experiment/ExperimentGeraLandingExecutionDetailPage";
import ExperimentGeraLandingProvisionalHtmlPage from "./pages/experiment/ExperimentGeraLandingProvisionalHtmlPage";
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
import AgentListPage from "./pages/agent/AgentListPage";
import NewAgentPage from "./pages/agent/NewAgentPage";
import EditAgentPage from "./pages/agent/EditAgentPage";
import AgentThemePage from "./pages/agent/AgentThemePage";
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
import EmailSettingsPage from "./pages/settings/EmailSettingsPage";
import WhatsAppConsolePage from "./pages/whatsapp/WhatsAppConsolePage";
import LeadPortalExperimentMetricsPage from "./pages/leadPortal/LeadPortalExperimentMetricsPage";
import LeadPortalImagesPage from "./pages/leadPortal/LeadPortalImagesPage";
import LeadPortalImagePackageDetailPage from "./pages/leadPortal/LeadPortalImagePackageDetailPage";
import LeadPortalSimpleFormStylesPage from "./pages/leadPortal/LeadPortalSimpleFormStylesPage";
import LeadPortalImagePromptPage from "./pages/leadPortal/LeadPortalImagePromptPage";
import LeadPortalFormResponsesPage from "./pages/leadPortal/LeadPortalFormResponsesPage";
import LeadPortalEmailTemplatePage from "./pages/leadPortal/LeadPortalEmailTemplatePage";
import PaymentsDashboardPage from "./pages/payments/PaymentsDashboardPage";
import PaymentDetailPage from "./pages/payments/PaymentDetailPage";
import PromptListPage from "./pages/promptTemplate/PromptListPage";
import NewPromptPage from "./pages/promptTemplate/NewPromptPage";
import EditPromptPage from "./pages/promptTemplate/EditPromptPage";
import PromptDomainListPage from "./pages/promptDomain/PromptDomainListPage";
import NewPromptDomainPage from "./pages/promptDomain/NewPromptDomainPage";
import EditPromptDomainPage from "./pages/promptDomain/EditPromptDomainPage";
import TargetingRecentQueriesPage from "./pages/targeting/TargetingRecentQueriesPage";
import OprmRoutinePage from "./pages/oprm/OprmRoutinePage";
import OprmOfferPage from "./pages/oprm/OprmOfferPage";
import OprmEvidencePage from "./pages/oprm/OprmEvidencePage";
import OprmFeedbackPage from "./pages/oprm/OprmFeedbackPage";
import OprmOperationsPage from "./pages/oprm/OprmOperationsPage";
import OprmOccupationCatalogPage from "./pages/oprm/OprmOccupationCatalogPage";
import OprmCnaeVolumePage from "./pages/oprm/OprmCnaeVolumePage";
import OprmPipelinePage from "./pages/oprm/OprmPipelinePage";
import OprmNicheResearchSeedBuilderDetailPage from "./pages/oprm/OprmNicheResearchSeedBuilderDetailPage";
import OprmEnrichedNicheDetailPage from "./pages/oprm/OprmEnrichedNicheDetailPage";
import MoisWorkspacePage from "./pages/mois/MoisWorkspacePage";
import MoisReferenceIntakePage from "./pages/mois/MoisReferenceIntakePage";
import MoisExtractionPage from "./pages/mois/MoisExtractionPage";
import MoisLibraryPage from "./pages/mois/MoisLibraryPage";
import MoisSalesPagesLibraryPage from "./pages/mois/MoisSalesPagesLibraryPage";
import MoisSalesPagesPipelinePage from "./pages/mois/MoisSalesPagesPipelinePage";
import MoisSalesPageLibraryDetailPage from "./pages/mois/MoisSalesPageLibraryDetailPage";
import MoisComparisonPage from "./pages/mois/MoisComparisonPage";
import MoisOfferBuilderPage from "./pages/mois/MoisOfferBuilderPage";
import MoisResearchSourcesPage from "./pages/mois/MoisResearchSourcesPage";
import MoisAutoCollectionPage from "./pages/mois/MoisAutoCollectionPage";
import MoisAutomaticCollectionsPage from "./pages/mois/MoisAutomaticCollectionsPage";
import MoisCollectionJobDetailPage from "./pages/mois/MoisCollectionJobDetailPage";
import MdsWorkspacePage from "./pages/mds/MdsWorkspacePage";
import MdsRequestDetailPage from "./pages/mds/MdsRequestDetailPage";
import MdsArtifactsPage from "./pages/mds/MdsArtifactsPage";
import MdsReportPage from "./pages/mds/MdsReportPage";
import HotmartPage from "./pages/hotmart/HotmartPage";
import ClickbasePage from "./pages/clickbase/ClickbasePage";

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
                path="/products/:productId/sales-videos"
                element={<ProductSalesVideoPage />}
              />
              <Route
                path="/sales-videos/profiles/:profileId"
                element={<SalesVideoProfileDetailPage />}
              />
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
                <Route path="adset-workflow" element={<ExperimentAdSetWorkflowPage />} />
                <Route path="facebook-api-logs" element={<ExperimentFacebookApiLogsPage />} />
                <Route path="pipeline-jobs" element={<ExperimentPipelineJobsPage />} />
                <Route path="geralanding/stage-executions/:jobId" element={<ExperimentGeraLandingExecutionDetailPage />} />
                <Route path="geralanding/stage-executions/:jobId/provisional-html" element={<ExperimentGeraLandingProvisionalHtmlPage />} />
                <Route path="adset-workflow/jobs/:jobId" element={<ExperimentAdSetJobDetailPage />} />
                <Route path="framework-images" element={<ExperimentFrameworkImageDetailsPage />} />
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
              <Route path="/agents" element={<AgentListPage />} />
              <Route path="/agents/new" element={<NewAgentPage />} />
              <Route path="/agents/:id/edit" element={<EditAgentPage />} />
              <Route path="/agent-themes" element={<AgentThemePage />} />
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
              <Route path="/pipelines" element={<PipelineCrudPage />} />
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
              <Route
                path="/targeting/recent-queries"
                element={<TargetingRecentQueriesPage />}
              />
              <Route path="/oprm" element={<OprmCnaeVolumePage />} />
              <Route path="/mois" element={<MoisWorkspacePage />} />
              <Route path="/mois/references/new" element={<MoisReferenceIntakePage />} />
              <Route path="/mois/research-sources" element={<MoisResearchSourcesPage />} />
              <Route path="/mois/auto-collection" element={<MoisAutoCollectionPage />} />
              <Route path="/mois/automatic-collections" element={<MoisAutomaticCollectionsPage />} />
              <Route path="/mois/automatic-collections/:jobId" element={<MoisCollectionJobDetailPage />} />
              <Route path="/mois/extraction" element={<MoisExtractionPage />} />
              <Route path="/mois/library" element={<MoisLibraryPage />} />
              <Route path="/mois/sales-pages-library" element={<MoisSalesPagesLibraryPage />} />
              <Route path="/mois/sales-pages-library/pipeline" element={<MoisSalesPagesPipelinePage />} />
              <Route path="/mois/sales-pages-library/:pageId" element={<MoisSalesPageLibraryDetailPage />} />
              <Route path="/mois/comparison" element={<MoisComparisonPage />} />
              <Route path="/mois/builder" element={<MoisOfferBuilderPage />} />
              <Route path="/mds" element={<MdsWorkspacePage />} />
              <Route path="/mds/requests/:requestId" element={<MdsRequestDetailPage />} />
              <Route path="/mds/requests/:requestId/artifacts" element={<MdsArtifactsPage />} />
              <Route path="/mds/reports/:requestId" element={<MdsReportPage />} />
              <Route path="/hotmart" element={<HotmartPage />} />
              <Route path="/clickbase" element={<ClickbasePage />} />
              <Route
                path="/oprm/routine/:occupationSeedRef"
                element={<OprmRoutinePage />}
              />
              <Route
                path="/oprm/offer/:occupationSeedRef"
                element={<OprmOfferPage />}
              />
              <Route
                path="/oprm/evidence/:occupationSeedRef"
                element={<OprmEvidencePage />}
              />
              <Route
                path="/oprm/feedback/:occupationSeedRef"
                element={<OprmFeedbackPage />}
              />
              <Route
                path="/oprm/operations"
                element={<OprmOperationsPage />}
              />
              <Route path="/oprm/pipeline" element={<OprmPipelinePage />} />
              <Route path="/oprm/enriched-niches/profile/:profileId" element={<OprmEnrichedNicheDetailPage />} />
              <Route path="/oprm/pipeline/niche-research-seed-builder/:researchCycleId" element={<OprmNicheResearchSeedBuilderDetailPage />} />
              <Route
                path="/oprm/occupations"
                element={<OprmOccupationCatalogPage />}
              />
              <Route
                path="/oprm/cnaes-volume"
                element={<OprmCnaeVolumePage />}
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
                path="/lead-portal/form-responses"
                element={<LeadPortalFormResponsesPage />}
              />
              <Route
                path="/lead-portal/images"
                element={<LeadPortalImagesPage />}
              />
              <Route
                path="/lead-portal/images/:packageId"
                element={<LeadPortalImagePackageDetailPage />}
              />
              <Route
                path="/lead-portal/simple-form-styles"
                element={<LeadPortalSimpleFormStylesPage />}
              />
              <Route
                path="/lead-portal/image-prompts"
                element={<LeadPortalImagePromptPage />}
              />
              <Route
                path="/lead-portal/email-template"
                element={<LeadPortalEmailTemplatePage />}
              />
              <Route path="/payments" element={<PaymentsDashboardPage />} />
              <Route path="/payments/:id" element={<PaymentDetailPage />} />
              <Route path="/chat-dialogs" element={<ChatDialogListPage />} />
              <Route path="/chat-dialogs/new" element={<NewChatDialogPage />} />
              <Route path="/prompt-entities" element={<PromptEntitiesPage />} />
              <Route path="/prompt-domains" element={<PromptDomainListPage />} />
              <Route path="/prompt-domains/new" element={<NewPromptDomainPage />} />
              <Route path="/prompt-domains/:id/edit" element={<EditPromptDomainPage />} />
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
              <Route path="/prompts" element={<PromptListPage />} />
              <Route path="/prompts/new" element={<NewPromptPage />} />
              <Route path="/prompts/:id/edit" element={<EditPromptPage />} />
              <Route
                path="/facebook-campaigns"
                element={<FacebookCampaignExperimentsPage />}
              />
              <Route
                path="/facebook-campaigns/ready"
                element={<FacebookExperimentsReadyPage />}
              />
              <Route path="/whatsapp" element={<WhatsAppConsolePage />} />
              <Route
                path="/settings/privacy-policy"
                element={<PrivacyPolicySettingsPage />}
              />
              <Route
                path="/settings/email-service"
                element={<EmailSettingsPage />}
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
