import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { HypothesisFrameworkSection } from "./types";

export interface FrameworkGenerationJob {
  id: string;
  hypothesisId: string;
  section: HypothesisFrameworkSection;
  status: string;
  stage: string;
  requestBodyJson?: string;
  customInstructions?: string;
  errorMessage?: string;
  createdAt?: string;
  startedAt?: string;
  finishedAt?: string;
}

export function useFrameworkGenerationJobs(hypothesisId?: string) {
  return useQuery({
    queryKey: ["hypothesis-framework-jobs", hypothesisId],
    enabled: Boolean(hypothesisId),
    refetchInterval: 5000,
    queryFn: async () => {
      if (!hypothesisId) return [];
      const { data } = await axios.get<FrameworkGenerationJob[]>(
        `/api/hypotheses/${hypothesisId}/framework/jobs`,
        { params: { size: 50 } },
      );
      return data ?? [];
    },
  });
}
