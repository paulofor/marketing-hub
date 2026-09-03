import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type VideoProductionCycle = {
  id: number;
  videoProjectId: number;
  status: string;
  budgetLimitUsd: number;
  knownCostUsd: number;
  learningObjective: string;
  successCriterion: string;
  providerPreflight?: {
    id: number;
    status: "PENDING" | "READY" | "READY_WITH_BLOCKER" | "BLOCKED" | "EXPIRED";
    productionProfile: "DRAFT_INSTAGRAM" | "FINAL_CAMPAIGN";
    aggregatorName?: string;
    accountKey?: string;
    routerConfigId?: string;
    payloadSha256?: string;
    selectedRoutesJson?: string;
    estimatedCredits?: number;
    estimatedCostUsd?: number;
    maximumAuthorizedCredits?: number;
    maximumAuthorizedCostUsd?: number;
    officialBalanceCredits?: number;
    reservedCreditsSnapshot?: number;
    availableCreditsSnapshot?: number;
    maxMonthlyCreditSpend?: number;
    quotaSnapshotJson?: string;
    failureCode?: string;
    failureDetail?: string;
    sourceUrl?: string;
    rechargeUrl?: string;
    observedAt?: string;
    expiresAt?: string;
    reservation?: {
      id: number;
      status: "RESERVED" | "CONSUMING" | "SETTLED" | "RELEASED";
      reservedCredits: number;
      reservedCostUsd: number;
      actualCredits?: number;
      actualCostUsd?: number;
      expiresAt: string;
      reservedAt: string;
      settledAt?: string;
      releasedAt?: string;
    };
  };
  financialDecision?: string;
  financialReason?: string;
  recommendedAggregator?: string;
  recommendedRoute?: string;
  estimatedCostUsd?: number;
  costBenefitBasis?: string;
  creditAction?: "NO_PURCHASE" | "RECHARGE_REQUIRED" | "BLOCKED_UNKNOWN";
  recommendedRechargeCredits?: number;
  rechargeUrl?: string;
  salesVideoJobId?: number;
  lastFailedJobId?: number;
  lastApolloFailureCode?: string;
  lastApolloFailureDetail?: string;
  lastApolloFailureAt?: string;
  monitoredTaskCount: number;
  monitoredCredits: number;
  budgetMonitorStatus: "WATCHING" | "BLOCKED";
  budgetAlertCode?: string;
  budgetAlertDetail?: string;
  budgetAlertAt?: string;
  providerClipDurationSeconds: number;
  generationClipCount: number;
  editCutCount: number;
  textAppliedInPostProduction: boolean;
  agentTaskId?: number;
  createdAt: string;
};

/** Consulta os ciclos governados de Apolo e Plutus do projeto. */
export function useVideoProductionCycles(projectId?: number) {
  return useQuery({
    queryKey: ["video-production-cycles", projectId],
    queryFn: async () => {
      const { data } = await axios.get<VideoProductionCycle[]>(
        `/api/sales-videos/projects/${projectId}/autonomy/v1/cycles`,
      );
      return data;
    },
    enabled: Boolean(projectId),
    refetchInterval: projectId ? 10_000 : false,
  });
}

/** Abre um ciclo sem autorizar provider ou publicação. */
export function useCreateVideoProductionCycle(projectId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: {
      budgetLimitUsd: number;
      productionProfile: "DRAFT_INSTAGRAM" | "FINAL_CAMPAIGN";
      learningObjective: string;
      successCriterion: string;
      requestedBy: string;
    }) => {
      const { data } = await axios.post<VideoProductionCycle>(
        "/api/sales-videos/autonomy/v1/cycles",
        { videoProjectId: projectId, ...payload },
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["video-production-cycles", projectId],
      }),
  });
}
