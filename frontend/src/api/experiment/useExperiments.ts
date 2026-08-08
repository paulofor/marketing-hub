import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FacebookPageSummary {
  id: number;
  accountId: number;
  pageId: string;
  name: string;
}

export interface InstagramAccountSummary {
  id: number;
  handle: string;
  code: string;
  name: string;
}

export type ExperimentStage = "AD" | "LANDING" | "SAMPLE" | "SALES";
export type ExperimentType =
  | "NICHE_TEST"
  | "LOW_TICKET_PRODUCT"
  | "PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL"
  | "FAKE_EXPERIMENT";
export type ExperimentCreationSource = "SYSTEM_FLOW" | "MANUAL_FLOW";
export type ProductAiSubtype =
  | "AI_VISUAL_PREVIEW"
  | "AI_PERSONALIZED_SAMPLE"
  | "AI_TRANSFORMATION_SIMULATOR"
  | "AI_VISUAL_ASSET_PACK"
  | "AI_IDENTITY_AVATAR_PRODUCT"
  | "AI_REPORT_VISUAL_EVIDENCE";
export type ExperimentCampaignObjective = "LEADS" | "TRAFFIC" | "SALES";

export interface FacebookInstantFormSummary {
  id: number;
  hypothesisId: string;
  facebookPageId: number;
  facebookPageExternalId: string;
  facebookPageName: string;
  facebookFormId: string | null;
  name: string;
  status?: string | null;
  locale?: string | null;
  leadsCount?: number | null;
  createdTime?: string | null;
  updatedTime?: string | null;
  followUpActionUrl?: string | null;
  privacyPolicyUrl?: string | null;
  model?: string | null;
  prompt?: string | null;
  approved?: boolean;
  approvedAt?: string | null;
  published?: boolean;
  publishedAt?: string | null;
}

export interface ExperimentCampaignMetric {
  dateStart?: string | null;
  dateStop?: string | null;
  reach?: number | null;
  impressions?: number | null;
  clicks?: number | null;
  leads?: number | null;
  spend?: number | null;
  revenue?: number | null;
  cpc?: number | null;
  cpl?: number | null;
  lastSyncedAt?: string | null;
  lastSyncError?: string | null;
}

export interface ExperimentSessionDurationVariant {
  variantKey?: string | null;
  variantName?: string | null;
  sessions: number;
  averageVisibleMsPerSession: number;
}

export interface ExperimentSessionDurationSummary {
  totalSessions: number;
  averageVisibleMsPerSession: number;
  variants?: ExperimentSessionDurationVariant[] | null;
}

export interface Experiment {
  id: string;
  nicheId: number;
  hypothesisId: string;
  name: string;
  creationSource?: ExperimentCreationSource | null;
  hypothesis: string;
  singlePain?: string | null;
  freeReward?: string | null;
  funnelPromise?: string | null;
  primaryCta?: string | null;
  experimentType?: ExperimentType | null;
  productAiSubtype?: ProductAiSubtype | null;
  campaignObjective?: ExperimentCampaignObjective | null;
  pageId?: string | null;
  facebookPage?: FacebookPageSummary | null;
  facebookInstantForm?: FacebookInstantFormSummary | null;
  facebookPixelId?: string | null;
  facebookPixelCode?: string | null;
  facebookPixelCreatedAt?: string | null;
  facebookReleaseRequestedAt?: string | null;
  followUpActionUrl?: string | null;
  leadPortalFlowModel?: string | null;
  schemaFirstLeadPortalEnabled?: boolean;
  creativeTextPrompt?: string | null;
  campaignAngle?: string | null;
  adCopy?: string | null;
  adImageBriefing?: string | null;
  landingPageCopy?: string | null;
  landingPageWireframe?: string | null;
  landingPageImagePlanning?: string | null;
  landingPageImageAssets?: string | null;
  landingPageDesignPreset?: string | null;
  landingPageDeliverables?: string | null;
  landingPageHtml?: string | null;
  htmlGeraLanding?: string | null;
  learnedLessons?: string | null;
  commercialObjective?: string | null;
  currentOperationalFunction?: string | null;
  creativeImagePrompt?: string | null;
  instagramAccount?: InstagramAccountSummary | null;
  /**
   * KPI alvo em CPL. Mantém `kpiTarget` para compatibilidade com APIs
   * antigas que usavam este nome.
   */
  kpiTarget?: number;
  kpiTargetCpl?: number;
  stopLossCpl?: number | null;
  sampleSize?: number | null;
  baselineCvr?: number | null;
  targetCvr?: number | null;
  mdePercent?: number | null;
  dailyBudget?: number | null;
  unitPrice?: number | null;
  cost?: number | null;
  totalCost?: number | null;
  revenue?: number | null;
  auditableTotalCost?: number | null;
  legacyTotalCost?: number | null;
  unreconciledLegacyCost?: number | null;
  expense?: number | null;
  sessionDurationSummary?: ExperimentSessionDurationSummary | null;
  startDate: string | null;
  endDate: string | null;
  metricPresetId?: string | null;
  creativesToGenerate?: number | null;
  instantFormsToGenerate?: number | null;
  emailsToGenerate?: number | null;
  sampleEmailsToGenerate?: number | null;
  deliverablesToGenerate?: number | null;
  leadPortalFlowsToGenerate?: number | null;
  imagesPerPackage?: number | null;
  openImagesPerPackage?: number | null;
  compressedImagesPerPackage?: number | null;
  imageModelId?: number | null;
  imageModelName?: string | null;
  imageModelQualityId?: number | null;
  imageModelQualityName?: string | null;
  creativeApproved: boolean;
  status: string;
  platform: string;
  stage: ExperimentStage;
  creativeGenerationMode?: "DEFAULT" | "PIPELINE_ADS";
  creativeGenerationStatus?:
    "IDLE" | "REQUESTED" | "PROCESSING" | "COMPLETED" | "FAILED" | "TIMEOUT";
  creativeGenerationRequestedAt?: string | null;
  creativeGenerationStartedAt?: string | null;
  creativeGenerationFinishedAt?: string | null;
  creativeGenerationError?: string | null;
  lastStatusChangeReason?: string | null;
  lastStatusChangeAction?: string | null;
  lastStatusChangedAt?: string | null;
  primaryVariable?: string | null;
  primaryMetric?: string | null;
  createdAt: string;
  updatedAt: string;
  journeyTemplateId?: number | null;
  journeyTemplateName?: string | null;
  leadPortalFlowId?: number | null;
  leadPortalFlowName?: string | null;
  leadPortalFlowSlug?: string | null;
  selectedSampleEmailId?: number | null;
  selectedSampleEmailSubject?: string | null;
  selectedSampleEmailPreviewText?: string | null;
  selectedSampleEmailCallToAction?: string | null;
  selectedSampleEmailModel?: string | null;
  selectedSampleEmailUpdatedAt?: string | null;
  campaignMetric?: ExperimentCampaignMetric | null;
}

export function useExperiments() {
  return useQuery({
    queryKey: ["experiments"],
    queryFn: async () => {
      const { data } = await axios.get<Experiment[]>("/api/experiments");
      return data;
    },
  });
}

export interface ExperimentSummaryPage {
  items: Experiment[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export function useExperimentSummary(
  page: number,
  size: number,
  filters: { search?: string; status?: string; nicheId?: number },
) {
  return useQuery({
    queryKey: ["experiments-summary", page, size, filters],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentSummaryPage | Experiment[]>(
        "/api/experiments/summary",
        { params: { page, size, ...filters } },
      );
      if (Array.isArray(data)) {
        const finalized = new Set([
          "FINISHED",
          "VALIDATED",
          "INVALIDATED",
          "INCONCLUSIVE",
          "FAILED",
        ]);
        const filtered = data
          .filter((item) => !finalized.has(item.status))
          .sort((left, right) => {
            const running = Number(right.status === "RUNNING") - Number(left.status === "RUNNING");
            if (running !== 0) return running;
            return Number(right.id) - Number(left.id);
          });
        const items = filtered.slice(page * size, page * size + size);
        return {
          items,
          totalElements: filtered.length,
          totalPages: Math.ceil(filtered.length / size),
          page,
          size,
        };
      }
      return data;
    },
  });
}
