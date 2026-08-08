import axios from "axios";
import { useMutation, useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";
import type { GeneratedImageResult } from "./useGenerateImage";

export type RecentImageGeneration = {
  jobId: string;
  batchJobId: string;
  model: string;
  serviceTier: string;
  outputFormat: string;
  prompt: string;
  generatedAt: string;
};

type Context = {
  productId?: number;
  commercialPlanId?: number;
  experimentId?: number;
};

function params(context: Context) {
  return {
    productId: context.productId,
    commercialPlanId: context.commercialPlanId,
    ...(context.experimentId ? { experimentId: context.experimentId } : {}),
  };
}

export function useRecentImageGenerations(context: Context) {
  return useQuery({
    queryKey: ["image-generator", "recent", context],
    enabled: Boolean(context.productId && context.commercialPlanId),
    queryFn: async () => {
      const { data } = await axios.get<RecentImageGeneration[]>(
        buildApiUrl("/api/image-generator/generations/recent"),
        { params: params(context) },
      );
      return data;
    },
  });
}

export function useRecoverImageGeneration() {
  return useMutation({
    mutationFn: async ({ jobId, ...context }: Context & { jobId: string }) => {
      const { data } = await axios.get<GeneratedImageResult>(
        buildApiUrl(`/api/image-generator/generations/${jobId}`),
        { params: params(context) },
      );
      return data;
    },
  });
}
