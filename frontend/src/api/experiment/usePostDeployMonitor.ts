import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";

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
  averageVisibleMsPerSession: number;
  lastEventAt?: string | null;
  events: Record<string, number>;
  experienceVersions: PostDeployPdeExperienceVersion[];
  trafficSources: PostDeployPdeTrafficSource[];
  deviceBreakdown: PostDeployPdeDevice[];
  screenSizeBreakdown: PostDeployPdeScreenSize[];
  recentJourneys: PostDeployPdeSessionJourney[];
}

export interface PostDeployPdeDevice {
  deviceType: "mobile" | "desktop" | "tablet" | string;
  label: string;
  sessions: number;
  percentage: number;
}

export interface PostDeployPdeScreenSize {
  screenSize: string;
  label: string;
  width?: number | null;
  height?: number | null;
  sessions: number;
  percentage: number;
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

export interface PostDeployPdeTrafficSource {
  trafficChannel: string;
  utmSource: string;
  utmMedium: string;
  utmCampaign: string;
  utmContent: string;
  sessions: number;
  pdeEntries: number;
  firstInteractionClicks: number;
  loginStarted: number;
  paywallViewed: number;
  checkoutStarted: number;
  subscriptionApproved: number;
  firstInteractionRate: number;
  paywallRate: number;
  checkoutRate: number;
  purchaseRate: number;
  totalVisibleMs: number;
  lastEventAt?: string | null;
}

export interface PostDeployPdeSessionJourney {
  sessionId: string;
  visitorId?: string | null;
  clientIp?: string | null;
  firstEventAt?: string | null;
  lastEventAt?: string | null;
  totalVisibleMs: number;
  maxScrollDepthPercent: number;
  screenNames: string[];
  sectionIds: string[];
  fieldFocused: boolean;
  fieldInputStarted: boolean;
  fieldFilled: boolean;
  ctaClicked: boolean;
  loginStarted: boolean;
  loginCompleted: boolean;
  paywallViewed: boolean;
  checkoutStarted: boolean;
  subscriptionApproved: boolean;
  abandonmentPoint: string;
  lastEventType?: string | null;
  lastActionName?: string | null;
}

export type PdeProductionSlotStatus =
  | "PLANNED"
  | "READY"
  | "ACTIVE"
  | "PAUSED"
  | "RETIRED";

export interface PostDeployPdeProductionSlot {
  id: number;
  slotCode: string;
  productSlug: string;
  domain: string;
  publicUrl: string;
  backendUrl?: string | null;
  experienceVersion: string;
  targetEnvironment: string;
  status: PdeProductionSlotStatus;
  sourceExperimentId?: number | null;
  notes?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface SavePdeProductionSlotRequest {
  slotCode: string;
  productSlug: string;
  domain: string;
  publicUrl?: string;
  backendUrl?: string;
  experienceVersion: string;
  targetEnvironment?: string;
  status?: PdeProductionSlotStatus;
  sourceExperimentId?: number;
  notes?: string;
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
  pdeProductionSlots: PostDeployPdeProductionSlot[];
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

export function useSavePdeProductionSlot(experimentId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (variables: SavePdeProductionSlotRequest) => {
      const { data } = await axios.post<PostDeployPdeProductionSlot>(
        `/api/experiments/${experimentId}/post-deploy-monitor/pde/production-slots`,
        variables,
      );
      return data;
    },
    onSuccess: (slot) => {
      toast.success(`Slot PDE ${slot.slotCode} salvo.`);
      queryClient.invalidateQueries({
        queryKey: ["experiment", experimentId, "post-deploy-monitor"],
      });
    },
    onError: () => {
      toast.error("Não foi possível salvar o slot produtivo PDE agora.");
    },
  });
}
