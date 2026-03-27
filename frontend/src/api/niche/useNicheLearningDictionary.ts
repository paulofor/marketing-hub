import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { LearningInsightType } from "../experiment/useExperimentLearnings";

export interface LearningStatement {
  type: LearningInsightType;
  statement: string;
  evidence?: string | null;
  confidence?: string | null;
  experimentId?: number | null;
  experimentName?: string | null;
  completedAt?: string | null;
  metricSignal?: string | null;
}

export interface NicheLearningDictionary {
  updatedAt?: string | null;
  pains: LearningStatement[];
  results: LearningStatement[];
  mechanisms: LearningStatement[];
  proofs: LearningStatement[];
  offers: LearningStatement[];
}

export function useNicheLearningDictionary(nicheId?: number) {
  return useQuery({
    queryKey: ["niche-learning-dictionary", nicheId],
    queryFn: async () => {
      const { data } = await axios.get<NicheLearningDictionary>(
        `/api/niches/${nicheId}/learning/dictionary`,
      );
      return data;
    },
    enabled: typeof nicheId === "number" && nicheId > 0,
  });
}
