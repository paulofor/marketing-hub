import { useQuery } from "@tanstack/react-query";
import axios from "axios";

const STAGE_ENDPOINT_SEGMENT: Record<string, string> = {
  "landing-page-wireframe": "wireframe",
  "landing-page-copy": "copy",
  "landing-page-design-preset": "design-preset",
  "landing-page-image-planning": "image-prompts",
  "landing-page-image-generation": "image-generation",
  "landing-page-quality-review": "quality-review",
  "landing-page-deliverables": "deliverables",
};

function resolveStageExecutionsEndpointSegment(stageCode: string): string {
  return STAGE_ENDPOINT_SEGMENT[stageCode] ?? "wireframe";
}

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
    queryKey: [
      "geralanding-stage-executions",
      experimentId,
      stageCode,
      includeCompleted,
    ],
    enabled: Boolean(experimentId),
    refetchInterval: includeCompleted ? 10000 : 3000,
    queryFn: async () => {
      const { data } = await axios.get<GeraLandingStageExecutionItem[]>(
        `/api/experiments/${experimentId}/geralanding/${resolveStageExecutionsEndpointSegment(stageCode)}/stage-executions`,
        { params: { includeCompleted } },
      );
      return data;
    },
  });
}

export function useGeraLandingStageExecutionDetail(
  experimentId?: string,
  jobId?: string,
  stageCode = "landing-page-wireframe",
  options?: { enabled?: boolean; refetchInterval?: number | false },
) {
  return useQuery({
    queryKey: [
      "geralanding-stage-execution-detail",
      experimentId,
      jobId,
      stageCode,
    ],
    enabled: options?.enabled ?? Boolean(experimentId && jobId),
    refetchInterval: options?.refetchInterval,
    queryFn: async () => {
      const stageSegment = resolveStageExecutionsEndpointSegment(stageCode);
      const { data } = await axios.get<GeraLandingStageExecutionDetail>(
        `/api/experiments/${experimentId}/geralanding/${stageSegment}/stage-executions/${jobId}`,
      );
      return data;
    },
  });
}
