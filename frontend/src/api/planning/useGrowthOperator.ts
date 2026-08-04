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
  evidenceFingerprint?: string | null;
  alternativesJson?: string | null;
  diagnosisJson?: string | null;
  toolUsageJson?: string | null;
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

export interface GrowthOperatorMcpTool {
  name: string;
  description: string;
  accessMode: "SOMENTE_LEITURA" | "MUTACAO_GOVERNADA" | "APROVACAO_HUMANA";
  dataSource: string;
  parameters: Record<string, string>;
}

export interface GrowthOperatorTask {
  id: number;
  planId: number;
  sourceExecutionId: number;
  actionText: string;
  status: "OPEN" | "COMPLETED" | "CANCELLED";
  resolutionEvidence?: string | null;
  resolvedAt?: string | null;
  createdAt: string;
}

export function useGrowthOperatorTasks(planId?: number | null) {
  return useQuery({
    queryKey: ["growth-operator-tasks", planId],
    enabled: Boolean(planId),
    queryFn: async () => {
      const { data } = await axios.get<GrowthOperatorTask[]>(
        `/api/growth-operator/v1/commercial-plans/${planId}/tasks`,
      );
      return data;
    },
  });
}

export function useResolveGrowthOperatorTask(planId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      taskId,
      evidence,
    }: {
      taskId: number;
      evidence: string;
    }) => {
      const { data } = await axios.post<GrowthOperatorTask>(
        `/api/growth-operator/v1/commercial-plans/${planId}/tasks/${taskId}/resolve`,
        { evidence },
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["growth-operator-tasks", planId],
      }),
  });
}

export function useGrowthOperatorMcpTools() {
  return useQuery({
    queryKey: ["growth-operator-mcp-tools"],
    staleTime: 5 * 60_000,
    queryFn: async () => {
      const { data } = await axios.get<GrowthOperatorMcpTool[]>(
        "/api/growth-operator/v1/mcp-tools",
      );
      return data;
    },
  });
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
