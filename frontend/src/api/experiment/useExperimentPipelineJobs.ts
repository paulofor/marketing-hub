import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentPipelineGenerationJob {
  id: string;
  experimentId: number;
  section: string;
  status: string;
  stage: string;
  customInstructions?: string;
  errorMessage?: string;
  model?: string;
  prompt?: string;
  requestBodyJson?: string;
  rawResponse?: string;
  openAiResponseId?: string;
  createdAt?: string;
  startedAt?: string;
  finishedAt?: string;
}

export function useExperimentPipelineJobs(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-pipeline-jobs", experimentId],
    enabled: Boolean(experimentId),
    refetchInterval: 5000,
    queryFn: async () => {
      if (!experimentId) return [];
      const { data } = await axios.get<ExperimentPipelineGenerationJob[]>(
        `/api/experiments/${experimentId}/pipeline/jobs`,
        { params: { size: 50 } },
      );
      return data ?? [];
    },
  });
}
