import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentPipelineGenerationJobSummary {
  id: string;
  experimentId: number;
  section: string;
  status: string;
  stage: string;
  model?: string;
  errorMessage?: string;
  costUsd?: number;
  createdAt?: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface ExperimentPipelineGenerationJobDetail {
  id: string;
  experimentId: number;
  section: string;
  status: string;
  stage: string;
  model?: string;
  workerId?: string;
  customInstructions?: string;
  prompt?: string;
  requestBodyJson?: string;
  responseContent?: string;
  rawResponse?: string;
  errorMessage?: string;
  inputTokens?: number;
  outputTokens?: number;
  costUsd?: number;
  createdAt?: string;
  startedAt?: string;
  finishedAt?: string;
}

interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export type ExperimentPipelineJobHistoryPage =
  PageResponse<ExperimentPipelineGenerationJobSummary>;

interface UseExperimentPipelineJobHistoryParams {
  experimentId?: string;
  page: number;
  size?: number;
  section?: string;
}

export function useExperimentPipelineJobHistory({
  experimentId,
  page,
  size = 20,
  section,
}: UseExperimentPipelineJobHistoryParams) {
  return useQuery({
    queryKey: ["experiment-pipeline-job-history", experimentId, page, size, section],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      if (!experimentId) {
        return {
          content: [],
          totalPages: 0,
          totalElements: 0,
          number: 0,
          size,
        } as ExperimentPipelineJobHistoryPage;
      }
      const { data } = await axios.get<
        ExperimentPipelineJobHistoryPage
      >(`/api/experiments/${experimentId}/pipeline/jobs/history`, {
        params: {
          page,
          size,
          section: section || undefined,
        },
      });
      return data;
    },
    placeholderData: (previousData) => previousData,
  });
}

export function useExperimentPipelineJobDetail(
  experimentId?: string,
  jobId?: string,
) {
  return useQuery({
    queryKey: ["experiment-pipeline-job-detail", experimentId, jobId],
    enabled: Boolean(experimentId && jobId),
    queryFn: async () => {
      const { data } = await axios.get<ExperimentPipelineGenerationJobDetail>(
        `/api/experiments/${experimentId}/pipeline/jobs/${jobId}`,
      );
      return data;
    },
  });
}

export function useExperimentPipelineTotalCostUsd(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-pipeline-job-total-cost-usd", experimentId],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<number>(
        `/api/experiments/${experimentId}/pipeline/jobs/total-cost-usd`,
      );
      return data;
    },
  });
}
