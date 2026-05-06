import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface GeraLandingStageExecutionItem {
  idJob: string;
  status: string;
  executionRequestedAt: string;
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
  inputTokens?: number;
  outputTokens?: number;
  costUsd?: number;
}

export function useGeraLandingStageExecutions(
  experimentId: string,
  stageCode = "landing-page-wireframe",
) {
  return useQuery({
    queryKey: ["geralanding-stage-executions", experimentId, stageCode],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<GeraLandingStageExecutionItem[]>(
        `/api/experiments/${experimentId}/geralanding/stage-executions`,
        { params: { stageCode } },
      );
      return data;
    },
  });
}

export function useGeraLandingStageExecutionDetail(experimentId?: string, jobId?: string) {
  return useQuery({
    queryKey: ["geralanding-stage-execution-detail", experimentId, jobId],
    enabled: Boolean(experimentId && jobId),
    queryFn: async () => {
      const { data } = await axios.get<GeraLandingStageExecutionDetail>(
        `/api/experiments/${experimentId}/geralanding/stage-executions/${jobId}`,
      );
      return data;
    },
  });
}
