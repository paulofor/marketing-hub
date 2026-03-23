import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface ExperimentFunnelResetResponse {
  resetAt: string;
}

export function useResetExperimentFunnel(experimentId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ["experiment", experimentId, "funnel", "reset"],
    mutationFn: async () => {
      if (!experimentId) {
        throw new Error("ID do experimento é obrigatório para resetar o funil.");
      }
      const { data } = await axios.post<ExperimentFunnelResetResponse>(
        `/api/experiments/${experimentId}/funnel/reset`,
      );
      return data;
    },
    onSuccess: () => {
      if (!experimentId) return;
      queryClient.invalidateQueries({ queryKey: ["experiment", experimentId, "funnel"] });
    },
  });
}
