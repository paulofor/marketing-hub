import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface ClosePipelineJobsInput {
  experimentId: string;
  reason?: string;
}

export function useCloseExperimentPipelineJobs() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ experimentId, reason }: ClosePipelineJobsInput) => {
      const { data } = await axios.post<number>(
        `/api/experiments/${experimentId}/pipeline/jobs/close-open`,
        null,
        {
          params: { reason },
        },
      );
      return data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["experiment", variables.experimentId] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
      queryClient.invalidateQueries({ queryKey: ["experiment-pipeline-jobs", variables.experimentId] });
    },
  });
}
