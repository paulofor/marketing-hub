import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useGenerateFrameworkImages(experimentId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      if (!experimentId) {
        throw new Error("Experimento inválido para gerar imagens.");
      }
      const { data } = await axios.post(
        `/api/experiments/${experimentId}/framework-images/generate`,
      );
      return data;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["framework-image-statuses", experimentId],
        }),
        queryClient.invalidateQueries({
          queryKey: ["experiment-pipeline-jobs", experimentId],
        }),
      ]);
    },
  });
}
