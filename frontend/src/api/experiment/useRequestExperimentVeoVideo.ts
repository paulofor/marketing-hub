import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  ExperimentVideoAsset,
  RequestExperimentVeoVideoPayload,
} from "./useExperimentVideoAssets";

export function useRequestExperimentVeoVideo(experimentId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: RequestExperimentVeoVideoPayload) => {
      if (!experimentId) {
        throw new Error("Experimento inválido para criação de vídeo");
      }
      const { data } = await axios.post<ExperimentVideoAsset>(
        `/api/experiments/${experimentId}/video-assets/veo-render-requests`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["experiment-video-assets", experimentId],
      });
      queryClient.invalidateQueries({ queryKey: ["experiment", String(experimentId)] });
    },
  });
}
