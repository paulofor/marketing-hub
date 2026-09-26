import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { ExperimentVideoAsset } from "./useExperimentVideoAssets";

export interface UploadExperimentAdVideoInput {
  file: File;
  objective: string;
  primaryMetric: string;
  script: string;
  durationSeconds: number;
  hasAudio: boolean;
  visualSourceKey: string;
  visualSourceDescription: string;
  productionReference: string;
  visualSourceCreativeIds: number[];
  requiredForRelease: boolean;
}

/** Envia um MP4 finalizado e o mantém pendente de revisão comercial. */
export function useUploadExperimentAdVideo(experimentId: string | number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (input: UploadExperimentAdVideoInput) => {
      const form = new FormData();
      form.append("file", input.file);
      form.append("objective", input.objective);
      form.append("primaryMetric", input.primaryMetric);
      form.append("script", input.script);
      form.append("durationSeconds", String(input.durationSeconds));
      form.append("hasAudio", String(input.hasAudio));
      form.append("visualSourceKey", input.visualSourceKey);
      form.append("visualSourceDescription", input.visualSourceDescription);
      form.append("productionReference", input.productionReference);
      input.visualSourceCreativeIds.forEach((creativeId) =>
        form.append("visualSourceCreativeIds", String(creativeId)),
      );
      form.append("requiredForRelease", String(input.requiredForRelease));
      const { data } = await axios.post<ExperimentVideoAsset>(
        `/api/experiments/${experimentId}/video-assets/ad-uploads`,
        form,
      );
      return data;
    },
    onSuccess: async () => {
      await client.invalidateQueries({
        queryKey: ["experiment-video-assets", experimentId],
      });
      await client.invalidateQueries({
        queryKey: ["experiment-video-performance-dashboard", experimentId],
      });
      await client.invalidateQueries({
        queryKey: ["experiment-readiness", experimentId],
      });
    },
  });
}
