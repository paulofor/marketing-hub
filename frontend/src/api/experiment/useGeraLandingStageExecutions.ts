import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface GeraLandingStageExecutionItem {
  idJob: string;
  status: string;
  executionRequestedAt: string;
  costUsd?: number;
}

export interface GeraLandingStageExecutionDetail extends GeraLandingStageExecutionItem {
  experimentId: number;
  stageCode: string;
  createdAt?: string;
  processingStartedAt?: string;
  completedAt?: string;
  promptTemplateId?: string;
  promptContent?: string;
  prompt?: string;
  openAiRequestBody?: string;
  schemaJson?: string;
  promptMarkdownContent?: string;
  openAiJobId?: string;
  modelResponse?: string;
  provisionalHtml?: string;
  errorMessage?: string;
  errorDetail?: string;
  inputTokens?: number;
  outputTokens?: number;
  costUsd?: number;
}

export function useGeraLandingStageExecutions(
  experimentId: string,
  stageCode = "landing-page-wireframe",
  includeCompleted = true,
) {
  return useQuery({
    queryKey: ["geralanding-stage-executions", experimentId, stageCode, includeCompleted],
    enabled: Boolean(experimentId),
    refetchInterval: includeCompleted ? false : 10000,
    queryFn: async () => {
      const { data } = await axios.get<GeraLandingStageExecutionItem[]>(
        `/api/experiments/${experimentId}/geralanding/stage-executions`,
        { params: { stageCode, includeCompleted } },
      );
      return data;
    },
  });
}

export function useGeraLandingStageExecutionDetail(
  experimentId?: string,
  jobId?: string,
  options?: { enabled?: boolean; refetchInterval?: number | false },
) {
  return useQuery({
    queryKey: ["geralanding-stage-execution-detail", experimentId, jobId],
    enabled: options?.enabled ?? Boolean(experimentId && jobId),
    refetchInterval: options?.refetchInterval,
    queryFn: async () => {
      const { data } = await axios.get<GeraLandingStageExecutionDetail>(
        `/api/experiments/${experimentId}/geralanding/stage-executions/${jobId}`,
      );
      return data;
    },
  });
}
