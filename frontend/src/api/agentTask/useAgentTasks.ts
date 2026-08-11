import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { AgentTask, AgentTaskStatus, CreateAgentTaskPayload } from "./types";

/** Consulta a caixa de entrada segregada pela identidade técnica do agente. */
export function useAgentTasks(agentKey?: string) {
  return useQuery({
    queryKey: ["agent-tasks", agentKey],
    enabled: Boolean(agentKey),
    queryFn: async () =>
      (
        await axios.get<AgentTask[]>(
          `/api/agent-tasks/agents/${encodeURIComponent(agentKey!)}`,
        )
      ).data,
  });
}

/** Abre uma solicitação humana e atualiza a caixa do destinatário. */
export function useCreateAgentTask(agentKey?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateAgentTaskPayload) =>
      (await axios.post<AgentTask>("/api/agent-tasks", payload)).data,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["agent-tasks", agentKey] }),
  });
}

/** Evolui o estado de uma tarefa e recarrega a caixa correspondente. */
export function useUpdateAgentTaskStatus(agentKey?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      status,
    }: {
      id: number;
      status: AgentTaskStatus;
    }) =>
      (
        await axios.patch<AgentTask>(`/api/agent-tasks/${id}/status`, {
          status,
        })
      ).data,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["agent-tasks", agentKey] }),
  });
}
