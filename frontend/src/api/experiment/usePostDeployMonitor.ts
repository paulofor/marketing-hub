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
  lastEventAt?: string | null;
  events: Record<string, number>;
  experienceVersions: PostDeployPdeExperienceVersion[];
  trafficSources: PostDeployPdeTrafficSource[];
  recentJourneys: PostDeployPdeSessionJourney[];
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
  utmSource: string;
  utmCampaign: string;
  utmContent: string;
  sessions: number;
  pdeEntries: number;
  firstInteractionClicks: number;
  loginStarted: number;
  paywallViewed: number;
  checkoutStarted: number;
  subscriptionApproved: number;
  totalVisibleMs: number;
  lastEventAt?: string | null;
}

export interface PostDeployPdeSessionJourney {
  sessionId: string;
  visitorId?: string | null;
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

export interface PostDeployPdeDeployEnvironment {
  environment: string;
  available: boolean;
  status: string;
  errorMessage?: string | null;
  composeFile?: string | null;
  commitSha?: string | null;
  imageTag?: string | null;
  experienceVersion?: string | null;
  frontendUrl?: string | null;
  backendUrl?: string | null;
  frontendReachable: boolean;
  backendReachable: boolean;
  deployedAt?: string | null;
  services: PostDeployPdeDeployService[];
}

export interface PostDeployPdePromotionControl {
  homologAvailable: boolean;
  productionAvailable: boolean;
  productionBehind: boolean;
  productionUpToDate: boolean;
  productionDeployAvailable: boolean;
  productionDeployBlocked: boolean;
  statusLabel: string;
  recommendation: string;
  sourceCommitSha?: string | null;
  productionCommitSha?: string | null;
  targetEnvironment: string;
  workflowFile: string;
}

export interface PostDeployPdeProductionDeployResponse {
  accepted: boolean;
  status: string;
  message: string;
  targetEnvironment: string;
  workflowFile: string;
  sourceCommitSha?: string | null;
  requestedAt: string;
}

export interface PostDeployPdeDeployService {
  name: string;
  containerName: string;
  image?: string | null;
  publicPort?: number | null;
  targetPort?: number | null;
  role: string;
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
  pdePromotionControl: PostDeployPdePromotionControl;
  pdeDeployments: PostDeployPdeDeployEnvironment[];
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

export function useRequestPdeProductionDeploy(experimentId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (variables: { requestedBy?: string; sourceCommitSha?: string | null }) => {
      const { data } = await axios.post<PostDeployPdeProductionDeployResponse>(
        `/api/experiments/${experimentId}/post-deploy-monitor/pde/production-deploy`,
        variables,
      );
      return data;
    },
    onSuccess: (response) => {
      if (response.accepted) {
        toast.success(response.message);
      } else {
        toast.warn(response.message);
      }
      queryClient.invalidateQueries({
        queryKey: ["experiment", experimentId, "post-deploy-monitor"],
      });
    },
    onError: () => {
      toast.error("Não foi possível solicitar o deploy de produção agora.");
    },
  });
}
