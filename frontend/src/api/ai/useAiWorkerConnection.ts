import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export interface AiWorkerConnection {
  id: number;
  status:
    | "DISCONNECTED"
    | "REQUESTED"
    | "STARTING"
    | "AWAITING_CONFIRMATION"
    | "AUTHENTICATED"
    | "FAILED"
    | "EXPIRED";
  connected: boolean;
  workerAvailable: boolean;
  checkedAt: string | null;
  verificationUrl: string | null;
  userCode: string | null;
  requestedAt: string;
  expiresAt: string;
}

const key = ["ai-worker", "connection"];
export const connectionPending = (status?: string) =>
  ["REQUESTED", "STARTING", "AWAITING_CONFIRMATION"].includes(status ?? "");

/** Acompanha a prova do executor e a tentativa persistida no backend. */
export function useAiWorkerConnection() {
  return useQuery({
    queryKey: key,
    queryFn: async () =>
      (await axios.get<AiWorkerConnection>("/api/ai/worker-connection")).data,
    refetchInterval: (query) =>
      connectionPending(query.state.data?.status) ? 2_000 : 15_000,
  });
}

/** Reutiliza a tentativa ativa e atualiza imediatamente o painel após o comando. */
export function useConnectAiWorker() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      (await axios.post<AiWorkerConnection>("/api/ai/worker-connection")).data,
    onSuccess: (data) => {
      client.setQueryData(key, data);
    },
  });
}
