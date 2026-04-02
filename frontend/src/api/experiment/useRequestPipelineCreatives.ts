import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useRequestPipelineCreatives(experimentId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      if (!experimentId) throw new Error("Experiment not provided");
      const { data } = await axios.post(`/api/experiments/${experimentId}/pipeline/ads`);
      return data;
    },
    onSuccess: () => {
      if (!experimentId) return;
      queryClient.invalidateQueries({ queryKey: ["experiment", experimentId] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
      queryClient.invalidateQueries({ queryKey: ["creatives", experimentId] });
    },
  });
}
