import axios from "axios";
import { useMutation } from "@tanstack/react-query";

export type ImageGenerationResponse = {
  jobId: string;
  model: string;
  serviceTier: string;
  outputFormat: string;
  imageBase64: string;
  generatedAt: string;
};

type ImageGenerationPayload = {
  prompt: string;
};

export function useGenerateImage() {
  return useMutation({
    mutationFn: async (payload: ImageGenerationPayload) => {
      const { data } = await axios.post<ImageGenerationResponse>(
        "/api/image-generator/generations",
        payload,
      );
      return data;
    },
  });
}
