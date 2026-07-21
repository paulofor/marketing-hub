import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type PostDeployMonitorDecision =
  | "WAITING_DATA"
  | "KEEP_MONITORING"
  | "PAUSE_AND_FIX"
  | "SCALE_GRADUALLY"
  | "TECHNICAL_ATTENTION";

export interface PostDeployMetaAdsSummary {
  dateStart?: string | null;
  dateStop?: string | null;
  reach?: number | null;
  impressions?: number | null;
  clicks?: number | null;
  leads?: number | null;
  spend?: number | null;
  cpc?: number | null;
  cpl?: number | null;
  ctrPercent?: number | null;
  lastSyncedAt?: string | null;
  lastSyncError?: string | null;
}

export interface PostDeployPdeSummary {
  available: boolean;
  status: string;
  errorMessage?: string | null;
  currentExperienceVersion?: string | null;
  totalEvents: number;
  uniqueVisitors: number;
  sessions: number;
  pdeEntries: number;
  pageViews: number;
  presenceMapClicks: number;
  diagnosticClicks: number;
  fieldFilled: number;
  loginStarted: number;
  loginCompleted: number;
  paywallViewed: number;
  subscriptionClicked: number;
  checkoutStarted: number;
  subscriptionApproved: number;
  totalVisibleMs: number;
  lastEventAt?: string | null;
  events: Record<string, number>;
  experienceVersions: PostDeployPdeExperienceVersion[];
}

export interface PostDeployPdeExperienceVersion {
  experienceVersion: string;
  totalEvents: number;
  sessions: number;
  pdeEntries: number;
  firstInteractionClicks: number;
  loginStarted: number;
  paywallViewed: number;
  checkoutIntent: number;
  subscriptionApproved: number;
}

export interface PostDeployFacebookLogSummary {
  totalLogs: number;
  errorLogs: number;
  lastLogAt?: string | null;
  recentErrors: string[];
}

export interface PostDeployMonitorResponse {
  experimentId: number;
  productSlug: string;
  generatedAt: string;
  decision: PostDeployMonitorDecision;
  decisionLabel: string;
  recommendation: string;
  metaAds: PostDeployMetaAdsSummary;
  pde: PostDeployPdeSummary;
  logs: PostDeployFacebookLogSummary;
  alerts: string[];
}

export function usePostDeployMonitor(
  experimentId?: string,
  productSlug = "metodo-musa-7-dias",
) {
  return useQuery<PostDeployMonitorResponse>({
    queryKey: ["experiment", experimentId, "post-deploy-monitor", productSlug],
    enabled: Boolean(experimentId),
    refetchInterval: 60_000,
    queryFn: async () => {
      const { data } = await axios.get<PostDeployMonitorResponse>(
        `/api/experiments/${experimentId}/post-deploy-monitor`,
        { params: { productSlug } },
      );
      return data;
    },
  });
}
