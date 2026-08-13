import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface CodexAuthReconnect {
  id: number;
  agentId: number;
  agentKey: string;
  status:
    | "REQUESTED"
    | "STARTING"
    | "AWAITING_CONFIRMATION"
    | "AUTHENTICATED"
    | "FAILED";
  verificationUrl?: string | null;
  userCode?: string | null;
  requestedBy: string;
  detail?: string | null;
  requestedAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
}

/** Consulta a verdade persistida da última reconexão do agente. */
export function useCodexAuthReconnect(agentId?: number) {
  return useQuery({
    queryKey: ["agents", agentId, "codex-auth-reconnect"],
    enabled: Boolean(agentId),
    queryFn: async () =>
      (
        await axios.get<CodexAuthReconnect | null>(
          `/api/agents/work-monitor/${agentId}/codex-auth/reconnections/current`,
        )
      ).data,
    refetchInterval: (query) =>
      ["REQUESTED", "STARTING", "AWAITING_CONFIRMATION"].includes(
        query.state.data?.status ?? "",
      )
        ? 2_000
        : false,
  });
}

/** Solicita reconexão e atualiza imediatamente o acompanhamento da tela. */
export function useStartCodexAuthReconnect() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (agentId: number) =>
      (
        await axios.post<CodexAuthReconnect>(
          `/api/agents/work-monitor/${agentId}/codex-auth/reconnections`,
        )
      ).data,
    onSuccess: (data) =>
      queryClient.setQueryData(
        ["agents", data.agentId, "codex-auth-reconnect"],
        data,
      ),
  });
}
