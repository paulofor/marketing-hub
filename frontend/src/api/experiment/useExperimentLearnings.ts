import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ExperimentStage } from "./useExperiments";

export type LearningInsightType =
  | "PAIN"
  | "RESULT"
  | "MECHANISM"
  | "PROOF"
  | "OFFER";

export interface LearningInsight {
  type: LearningInsightType;
  statement: string;
  evidence?: string | null;
  confidence?: string | null;
  stage?: ExperimentStage | null;
  primaryMetric?: string | null;
}

export interface LearningSuggestion {
  title: string;
  rationale?: string | null;
  stage?: ExperimentStage | null;
  primaryMetric?: string | null;
  priority?: string | null;
}

export interface ExperimentLearning {
  id: number;
  experimentId: number;
  requestId: number;
  nicheId: number;
  hypothesisId?: string | null;
  stage?: ExperimentStage | null;
  primaryMetric?: string | null;
  metricSignal?: string | null;
  summary?: string | null;
  whatWorked?: string | null;
  whatBlocked?: string | null;
  nextTest?: string | null;
  completedAt?: string | null;
  createdAt?: string | null;
  insights: LearningInsight[];
  suggestions: LearningSuggestion[];
}

export function useExperimentLearnings(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-learnings", experimentId],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentLearning[]>(
        `/api/experiments/${experimentId}/learnings`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
  });
}
