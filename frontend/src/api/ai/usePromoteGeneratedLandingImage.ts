import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

type PromoteGeneratedLandingImagePayload = {
  experimentId: number;
  jobId: string;
  slotId: "hero-media-img" | "prova-img";
};

/** Aplica uma geração persistida ao rascunho da landing sem publicar a página. */
export function usePromoteGeneratedLandingImage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: PromoteGeneratedLandingImagePayload) => {
      const { data } = await axios.post(
        `/api/image-generator/generations/${payload.jobId}/landing-assets`,
        { experimentId: payload.experimentId, slotId: payload.slotId },
      );
      return data;
    },
    onSuccess: (_data, payload) => {
      queryClient.invalidateQueries({
        queryKey: ["experiment", String(payload.experimentId)],
      });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
