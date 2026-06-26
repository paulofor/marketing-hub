import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useRequestPipelineCreatives(experimentKey?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      if (!experimentKey) throw new Error("Experimento não informado");
      const { data } = await axios.post(
        "/api/internal/aiworker/geracaoanuncios/v1/texto/stage-executions/start",
        null,
        { params: { experimentKey } },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["experiment", experimentKey],
      });
      queryClient.invalidateQueries({ queryKey: ["creatives", experimentKey] });
    },
  });
}
