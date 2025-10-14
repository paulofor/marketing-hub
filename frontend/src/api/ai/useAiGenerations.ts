import { keepPreviousData, useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { PageResponse } from "../journey/types";
import type { UseQueryResult } from "@tanstack/react-query";

export interface AiGeneration {
  id: number;
  domain: string;
  referenceId?: string | null;
  model?: string | null;
  prompt?: string | null;
  rawResponse?: string | null;
  inputTokens?: number | null;
  outputTokens?: number | null;
  costUsd: number;
  createdAt: string;
}

export interface AiGenerationQueryParams {
  page?: number;
  size?: number;
  domain?: string;
}

export function useAiGenerations(
  params: AiGenerationQueryParams,
): UseQueryResult<PageResponse<AiGeneration>> {
  return useQuery<PageResponse<AiGeneration>, Error>({
    queryKey: ["ai-generations", params],
    queryFn: async () => {
      const { data } = await axios.get<PageResponse<AiGeneration>>("/api/ai/generations", {
        params,
      });
      return data;
    },
    placeholderData: keepPreviousData,
  });
}
