import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface AgentExecutorOperation {
  id: number;
  agentId: number;
  agentKey: string;
  operationType: "UPDATE" | "RESTART";
  status: "REQUESTED" | "RUNNING" | "COMPLETED" | "FAILED";
  requestedBy: string;
  requestedAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  detail?: string | null;
}

/** Consulta o último comando persistido do executor. */
export function useAgentExecutorOperation(agentId?: number) {
  return useQuery({
    queryKey: ["agents", agentId, "executor-operation"],
    enabled: Boolean(agentId),
    queryFn: async () =>
      (
        await axios.get<AgentExecutorOperation | null>(
          `/api/agents/work-monitor/${agentId}/executor-operations/current`,
        )
      ).data,
    refetchInterval: (query) =>
      ["REQUESTED", "RUNNING"].includes(query.state.data?.status ?? "")
        ? 2_000
        : false,
  });
}

/** Solicita atualização ou reinício e acompanha o resultado persistido. */
export function useStartAgentExecutorOperation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      agentId,
      operation,
    }: {
      agentId: number;
      operation: "UPDATE" | "RESTART";
    }) =>
      (
        await axios.post<AgentExecutorOperation>(
          `/api/agents/work-monitor/${agentId}/executor-operations/${operation}`,
        )
      ).data,
    onSuccess: (data) =>
      queryClient.setQueryData(
        ["agents", data.agentId, "executor-operation"],
        data,
      ),
  });
}
