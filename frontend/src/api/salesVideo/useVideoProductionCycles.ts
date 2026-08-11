import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type VideoProductionCycle = {
  id: number;
  videoProjectId: number;
  status: string;
  budgetLimitUsd: number;
  knownCostUsd: number;
  financialDecision?: string;
  financialReason?: string;
  salesVideoJobId?: number;
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
  });
}

/** Abre um ciclo sem autorizar provider ou publicação. */
export function useCreateVideoProductionCycle(projectId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: {
      budgetLimitUsd: number;
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
