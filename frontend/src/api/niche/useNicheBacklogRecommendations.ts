import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ExperimentStage } from "../experiment/useExperiments";

export interface BacklogRecommendation {
  title: string;
  rationale?: string | null;
  stage?: ExperimentStage | null;
  primaryMetric?: string | null;
  priority?: string | null;
  experimentId?: number | null;
  experimentName?: string | null;
  completedAt?: string | null;
}

export function useNicheBacklogRecommendations(nicheId?: number) {
  return useQuery({
    queryKey: ["niche-learning-recommendations", nicheId],
    queryFn: async () => {
      const { data } = await axios.get<BacklogRecommendation[]>(
        `/api/niches/${nicheId}/learning/recommendations`,
      );
      return data;
    },
    enabled: typeof nicheId === "number" && nicheId > 0,
  });
}
