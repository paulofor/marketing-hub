import axios from "axios";
import { useMutation } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export type GeneratedImageResult = {
  jobId: string;
  model: string;
  serviceTier: string;
  outputFormat: string;
  imageBase64: string;
  variants?: GeneratedImageVariant[];
  generatedAt: string;
};

export type GeneratedImageVariant = {
  role: "original" | "web" | "mobile" | string;
  format: string;
  imageBase64: string;
  width: number;
  height: number;
  byteSize: number;
};

export type ImageGenerationResponse = {
  jobId: string;
  images: GeneratedImageResult[];
  failures?: GeneratedImageFailure[];
};

export type GeneratedImageFailure = {
  model: string;
  message: string;
  finishedAt: string;
};

type ImageGenerationPayload = {
  prompt: string;
};

export function useGenerateImage() {
  return useMutation({
    mutationFn: async (payload: ImageGenerationPayload) => {
      const { data } = await axios.post<ImageGenerationResponse>(
        buildApiUrl("/api/image-generator/generations"),
        payload,
        { timeout: 240000 },
      );
      return data;
    },
  });
}
