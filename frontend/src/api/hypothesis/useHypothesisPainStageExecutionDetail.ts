import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface HypothesisPainStageExecutionDetail {
  jobid: string;
  marketNicheId: number;
  hypothesisId?: string | null;
  stageCode: string;
  status: string;
  executionRequestedAt?: string;
  processingStartedAt?: string;
  completedAt?: string;
  promptTemplateId?: string;
  promptContent?: string;
  prompt?: string;
  promptMarkdownContent?: string;
  schemaJson?: string;
  openAiRequestBody?: string;
  openAiModel?: string;
  openAiJobId?: string;
  modelResponse?: string;
  errorMessage?: string;
  errorDetail?: string;
  inputTokens?: number;
  outputTokens?: number;
  costUsd?: number;
}

export function useHypothesisPainStageExecutionDetail(
  nicheId?: string,
  jobId?: string,
  stageSlug = "pain",
) {
  return useQuery({
    queryKey: ["hypothesis-stage-execution-detail", stageSlug, nicheId, jobId],
    enabled: Boolean(nicheId && jobId),
    queryFn: async () => {
      const { data } = await axios.get<HypothesisPainStageExecutionDetail>(
        `/api/niches/${nicheId}/hypothesis-pipeline/${stageSlug}/stage-executions/${jobId}`,
      );
      return data;
    },
  });
}
