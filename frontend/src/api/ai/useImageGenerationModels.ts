import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ImageOrientation = "SQUARE" | "PORTRAIT" | "LANDSCAPE";

export interface ImageGenerationPrice {
  id: number;
  orientation: ImageOrientation;
  width: number | null;
  height: number | null;
  sizeLabel: string | null;
  unitPriceUsd: number | null;
  preferred: boolean;
}

export interface ImageGenerationQuality {
  id: number;
  modelId: number;
  code: string;
  name: string;
  apiQuality?: string | null;
  defaultQuality: boolean;
  prices: ImageGenerationPrice[];
}

export interface ImageGenerationModel {
  id: number;
  code: string;
  name: string;
  provider: string;
  apiModel: string;
  description?: string | null;
  qualities: ImageGenerationQuality[];
}

export function useImageGenerationModels() {
  return useQuery<ImageGenerationModel[]>({
    queryKey: ["image-generation-models"],
    queryFn: async () => {
      const { data } = await axios.get<ImageGenerationModel[]>(
        "/api/image-generation/models",
      );
      return data;
    },
    staleTime: 5 * 60 * 1000,
  });
}
