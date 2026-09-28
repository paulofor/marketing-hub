import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { InstagramAccountSummary } from "./experiment/useExperiments";

export interface CampaignMetricSummary {
  dateStart: string | null;
  dateStop: string | null;
  impressions: number | null;
  clicks: number | null;
  leads: number | null;
  spend: number | null;
  cpc: number | null;
  cpl: number | null;
  lastSyncedAt: string | null;
  lastSyncError: string | null;
}

export interface LeadPortalFunnelSummary {
  formAccesses: number | null;
  formSubmissions: number | null;
}

export interface CampaignStrategySummary {
  objective: string | null;
  preset: string | null;
  maxSpendWithoutPurchase: number | null;
  minimumCheckoutRate: number | null;
  minimumLinkClicks: number | null;
  minimumImpressions: number | null;
  enabled: boolean;
}

export type CampaignDeliveryState =
  | "NOT_PUBLISHED"
  | "SCHEDULED"
  | "WINDOW_ENDED"
  | "PAUSE_PENDING"
  | "NOT_DELIVERING"
  | "AWAITING_STATUS_SYNC"
  | "STATUS_STALE"
  | "ELIGIBLE_NOT_CONFIRMED";

export interface CampaignOperationSummary {
  campaignId: string | null;
  configuredStatus: string | null;
  effectiveStatus: string | null;
  persistedStatus: string | null;
  deliveryState: CampaignDeliveryState;
  deliveringNow: boolean | null;
  windowStart: string | null;
  windowEnd: string | null;
  statusLastSyncedAt: string | null;
  budgetRemainingMinor: number | null;
  metricsFinalSyncedAt: string | null;
}

export interface ExperimentSummary {
  id: number;
  name: string;
  hypothesis: string;
  singlePain?: string | null;
  freeReward?: string | null;
  funnelPromise?: string | null;
  primaryCta?: string | null;
  experimentType?: string | null;
  campaignObjective?: string | null;
  followUpActionUrl?: string | null;
  kpiTargetCpl: number | null;
  startDate: string | null;
  endDate: string | null;
  nicheName: string | null;
  hypothesisTitle: string | null;
  missingConfiguration: string[];
  instagramAccount?: InstagramAccountSummary | null;
  leadPortalFunnel?: LeadPortalFunnelSummary | null;
  metrics?: CampaignMetricSummary | null;
  campaignStrategy?: CampaignStrategySummary | null;
  campaignOperation?: CampaignOperationSummary | null;
}

export function useFacebookCampaignExperiments(status: string) {
  return useQuery({
    queryKey: ["facebook-campaign-experiments", status],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentSummary[]>(
        "/api/facebook-campaigns/experiments",
        { params: { status } },
      );
      return data;
    },
  });
}
