import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  ExperimentVideoAsset,
  SalesVideoExecutionMode,
} from "./useExperimentVideoAssets";

export interface RequestExperimentVideoPostProductionPayload {
  requestedBy: string;
  executionMode?: SalesVideoExecutionMode;
  voiceOverScript?: string;
  captionText?: string;
  soundtrackStyle?: string;
  outputVariant?: string;
  createShortDerivatives?: boolean;
}

export function useRequestExperimentVideoPostProduction(
  experimentId?: string | number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (
      payload: RequestExperimentVideoPostProductionPayload,
    ) => {
      if (!experimentId) {
        throw new Error("Experimento inválido para pós-produção de vídeos");
      }
      const { data } = await axios.post<ExperimentVideoAsset[]>(
        `/api/experiments/${experimentId}/video-assets/post-production-requests`,
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
