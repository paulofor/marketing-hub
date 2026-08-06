import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentStrategistExecution {
  id: number;
  commercialPlanId: number;
  status: "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";
  authorityMode: "READ_ONLY_RESEARCH";
  researchQuestion: string;
  alternativesJson?: string | null;
  recommendationJson?: string | null;
  publicSourcesJson?: string | null;
  modelName?: string | null;
  estimatedCost?: number | null;
  errorMessage?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  createdAt: string;
}

export function useExperimentStrategistExecutions(planId?: number | null) {
  return useQuery({
    queryKey: ["experiment-strategist-executions", planId],
    enabled: Boolean(planId),
    refetchInterval: 15_000,
    queryFn: async () => {
      const { data } = await axios.get<ExperimentStrategistExecution[]>(
        `/api/experiment-strategist/v1/commercial-plans/${planId}/executions`,
      );
      return data;
    },
  });
}

export function useStartExperimentStrategist(planId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (researchQuestion: string) => {
      const { data } = await axios.post<ExperimentStrategistExecution>(
        `/api/experiment-strategist/v1/commercial-plans/${planId}/executions`,
        { researchQuestion },
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["experiment-strategist-executions", planId],
      }),
  });
}
