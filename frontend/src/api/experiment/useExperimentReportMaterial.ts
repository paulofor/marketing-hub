import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ExperimentCampaignMetric } from "./useExperiments";
import type { ExperimentFunnelStageSummary } from "./useExperimentFunnel";

export interface ExperimentReportMaterial {
  experiment?: ExperimentReportExperimentSnapshot | null;
  niche?: ExperimentReportNicheSnapshot | null;
  hypothesis?: ExperimentReportHypothesisSnapshot | null;
  creatives: ExperimentReportCreativeSnapshot[];
  creativeVariants: ExperimentReportCreativeVariantSnapshot[];
  landingPages: ExperimentReportLandingPageSnapshot[];
  leadPortalFlows: ExperimentReportLeadPortalFlowSnapshot[];
  instantForm?: ExperimentReportInstantFormSnapshot | null;
  campaignMetric?: ExperimentCampaignMetric | null;
  funnelStages: ExperimentFunnelStageSummary[];
}

export interface ExperimentReportExperimentSnapshot {
  id: number;
  name: string;
  status?: string | null;
  platform?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  dailyBudget?: number | null;
  kpiTargetCpl?: number | null;
  stopLossCpl?: number | null;
  sampleSize?: number | null;
  baselineCvr?: number | null;
  targetCvr?: number | null;
  mdePercent?: number | null;
  createdAt?: string | null;
}

export interface ExperimentReportNicheSnapshot {
  id: number;
  name: string;
  description?: string | null;
  interestList?: string[];
  roleList?: string[];
  behaviorList?: string[];
}

export interface ExperimentReportHypothesisSnapshot {
  id: string;
  title: string;
  promise?: string | null;
  problem?: string | null;
  persona?: string | null;
  mechanism?: string | null;
  uniqueMechanism?: string | null;
  entrega?: string | null;
}

export interface ExperimentReportCreativeSnapshot {
  id: number;
  headline?: string | null;
  primaryText?: string | null;
  description?: string | null;
  cta?: string | null;
  destinationUrl?: string | null;
  imageUrl?: string | null;
  videoId?: string | null;
  format?: string | null;
  status?: string | null;
  angles?: string[];
  emotionalTriggers?: string[];
  visualProofs?: string[];
}

export interface ExperimentReportCreativeVariantSnapshot {
  id: number;
  type?: string | null;
  assetUrl?: string | null;
  titles?: string[];
  descriptions?: string[];
  createdAt?: string | null;
}

export interface ExperimentReportLandingPageSnapshot {
  id: number;
  url?: string | null;
  type?: string | null;
  status?: string | null;
  createdAt?: string | null;
}

export interface ExperimentReportLeadPortalFlowSnapshot {
  id: number;
  name: string;
  slug: string;
  description?: string | null;
  model?: string | null;
  approved: boolean;
  previewImageUrl?: string | null;
  createdAt?: string | null;
  questions: ExperimentReportLeadPortalQuestionSnapshot[];
}

export interface ExperimentReportLeadPortalQuestionSnapshot {
  id: number;
  title: string;
  type?: string | null;
  required: boolean;
  options: string[];
}

export interface ExperimentReportInstantFormSnapshot {
  id: number;
  name: string;
  status?: string | null;
  shareLink?: string | null;
  followUpActionUrl?: string | null;
  privacyPolicyUrl?: string | null;
}

export function useExperimentReportMaterial(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-report-material", experimentId],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentReportMaterial>(
        `/api/experiments/${experimentId}/report-material`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
    staleTime: 60_000,
  });
}
