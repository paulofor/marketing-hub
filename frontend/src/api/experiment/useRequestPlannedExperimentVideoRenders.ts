import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  ExperimentVideoAsset,
  SalesVideoExecutionMode,
} from "./useExperimentVideoAssets";

export interface RequestPlannedExperimentVideoRendersPayload {
  requestedBy: string;
  executionMode?: SalesVideoExecutionMode;
  personaName?: string;
  personaStyle?: string;
  voiceStyle?: string;
  language?: string;
  requiredForRelease?: boolean;
}

export function useRequestPlannedExperimentVideoRenders(
  experimentId?: string | number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: RequestPlannedExperimentVideoRendersPayload) => {
      if (!experimentId) {
        throw new Error("Experimento inválido para renderização de vídeos");
      }
      const { data } = await axios.post<ExperimentVideoAsset[]>(
        `/api/experiments/${experimentId}/video-assets/planned-render-requests`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["experiment-video-assets", experimentId],
      });
      queryClient.invalidateQueries({
        queryKey: ["experiment-video-assets", "all"],
      });
      queryClient.invalidateQueries({
        queryKey: ["experiment", String(experimentId)],
      });
    },
  });
}
