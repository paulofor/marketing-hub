import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentVideoPerformanceSummary {
  approvedAssets: number;
  metaVideoCreatives: number;
  impressions: number;
  clicks: number;
  diagnosticStarts: number;
  checkoutAccesses: number;
  purchases: number;
  spend?: number | null;
  lastMetricAt?: string | null;
  recommendation?: string | null;
}

export interface ExperimentVideoPerformanceCreative {
  creativeId: string;
  creativeKind?: string | null;
  metaVideoId?: string | null;
  adId?: string | null;
  adName?: string | null;
  adStatus?: string | null;
  diagnosticStarts: number;
  checkoutAccesses: number;
  purchases: number;
}

export interface ExperimentVideoPerformanceAsset {
  assetId: number;
  slot?: string | null;
  reviewStatus?: string | null;
  status?: string | null;
  provider?: string | null;
  model?: string | null;
  assetUrl?: string | null;
  attributionLevel: "AD" | "EXPERIMENT" | string;
  metaCreatives: ExperimentVideoPerformanceCreative[];
  diagnosticStarts: number;
  checkoutAccesses: number;
  purchases: number;
}

export interface ExperimentVideoPerformanceCampaign {
  campaignId: string;
  campaignName?: string | null;
  status?: string | null;
  impressions: number;
  clicks: number;
  spend?: number | null;
  metricsLastSyncedAt?: string | null;
}

export interface ExperimentVideoPerformanceDashboard {
  summary: ExperimentVideoPerformanceSummary;
  assets: ExperimentVideoPerformanceAsset[];
  campaigns: ExperimentVideoPerformanceCampaign[];
}

export function useExperimentVideoPerformanceDashboard(
  experimentId?: string | number,
) {
  return useQuery({
    queryKey: ["experiment-video-performance-dashboard", experimentId],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<ExperimentVideoPerformanceDashboard>(
        `/api/experiments/${experimentId}/video-assets/performance-dashboard`,
      );
      return data;
    },
  });
}
