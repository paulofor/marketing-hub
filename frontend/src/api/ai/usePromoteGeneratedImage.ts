import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

type PromoteGeneratedImagePayload = {
  experimentId: number;
  jobId: string;
  model: string;
  prompt: string;
  format: string;
  imageBase64: string;
  headline: string;
  primaryText: string;
  description: string;
  cta: string;
  destinationUrl: string;
};

function decodeImage(imageBase64: string, format: string) {
  const bytes = Uint8Array.from(atob(imageBase64), (character) =>
    character.charCodeAt(0),
  );
  return new Blob([bytes], { type: `image/${format}` });
}

export function usePromoteGeneratedImage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: PromoteGeneratedImagePayload) => {
      const form = new FormData();
      form.append(
        "file",
        decodeImage(payload.imageBase64, payload.format),
        `${payload.jobId}.${payload.format}`,
      );
      form.append("model", payload.model);
      form.append("prompt", payload.prompt);
      form.append("category", "EXPERIMENT_CREATIVE");
      form.append("experimentId", String(payload.experimentId));

      const { data: asset } = await axios.post<{ url: string }>(
        "/api/assets",
        form,
      );
      const { data: creative } = await axios.post<{ id: number }>(
        `/api/experiments/${payload.experimentId}/creatives`,
        {
          format: "IMAGE",
          headline: payload.headline,
          primaryText: payload.primaryText,
          imageUrl: asset.url,
          description: payload.description,
          cta: payload.cta,
          destinationUrl: payload.destinationUrl,
          status: "DRAFT",
        },
      );
      const { data: submitted } = await axios.post(
        `/api/creatives/${creative.id}/agent-review/request`,
      );
      return submitted;
    },
    onSuccess: (_data, payload) => {
      queryClient.invalidateQueries({
        queryKey: ["creatives", String(payload.experimentId)],
      });
    },
  });
}
