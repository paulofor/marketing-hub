import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface FinancialAgentExecution {
  id: number;
  commercialPlanId: number;
  status: "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";
  authorityMode: "READ_ONLY_FINANCIAL_RECONCILIATION";
  financialSnapshot: string;
  reconciliationJson?: string | null;
  dailyReport?: string | null;
  model?: string | null;
  errorMessage?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  createdAt: string;
}

export function useFinancialAgentExecutions(planId?: number | null) {
  return useQuery({
    queryKey: ["financial-agent-executions", planId],
    enabled: Boolean(planId),
    refetchInterval: 15_000,
    queryFn: async () => {
      const { data } = await axios.get<FinancialAgentExecution[]>(
        `/api/financial-agent/v1/commercial-plans/${planId}/executions`,
      );
      return data;
    },
  });
}

export function useStartFinancialAgent(planId?: number | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const { data } = await axios.post<FinancialAgentExecution>(
        `/api/financial-agent/v1/commercial-plans/${planId}/executions`,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["financial-agent-executions", planId],
      }),
  });
}
