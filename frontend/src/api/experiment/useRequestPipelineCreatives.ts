import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useRequestPipelineCreatives(experimentId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      if (!experimentId) throw new Error("Experimento não informado");
      const { data } = await axios.post(
        `/api/internal/geraanuncio/v2/criativo/stage-executions/experiments/${experimentId}/start`,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment", experimentId] });
      queryClient.invalidateQueries({ queryKey: ["creatives", experimentId] });
    },
  });
}
