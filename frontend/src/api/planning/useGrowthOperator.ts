import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type GrowthOperatorStatus =
  "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";

export type GrowthOperatorDecision =
  "CONTINUE" | "ADJUST" | "STOP" | "WAIT_FOR_APPROVAL";

export interface GrowthOperatorExecution {
  id: number;
  commercialPlanId: number;
  weekNumber: number;
  status: GrowthOperatorStatus;
  authorityMode: "READ_ONLY_DIAGNOSIS";
  objective: string;
  blocker?: string | null;
  evidenceSnapshot?: string | null;
  alternativesJson?: string | null;
  diagnosisJson?: string | null;
  recommendedDecision?: GrowthOperatorDecision | null;
  recommendedAction?: string | null;
  dailyReport?: string | null;
  cycleNumber: number;
  automaticCycle: boolean;
  errorMessage?: string | null;
  model?: string | null;
  estimatedCost?: number | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  createdAt: string;
}

export function useGrowthOperatorExecutions(planId?: number | null) {
  return useQuery({
    queryKey: ["growth-operator-executions", planId],
    enabled: !!planId,
    refetchInterval: 15_000,
    queryFn: async () => {
      const { data } = await axios.get<GrowthOperatorExecution[]>(
        `/api/growth-operator/v1/commercial-plans/${planId}/executions`,
      );
      return data;
    },
  });
}

export function useStartGrowthOperator(planId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: { weekNumber: number; objective: string }) => {
      if (!planId) throw new Error("Planejamento não informado.");
      const { data } = await axios.post<GrowthOperatorExecution>(
        `/api/growth-operator/v1/commercial-plans/${planId}/executions`,
        payload,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["growth-operator-executions", planId],
      }),
  });
}
