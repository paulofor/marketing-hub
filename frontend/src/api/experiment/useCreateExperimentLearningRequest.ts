import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useCreateExperimentLearningRequest(experimentId?: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (requestedBy?: string) => {
      const payload = requestedBy?.trim()
        ? { requestedBy: requestedBy.trim() }
        : undefined;
      const { data } = await axios.post(
        `/api/experiments/${experimentId}/learning-requests`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["experiment-learning-requests", experimentId],
      });
      queryClient.invalidateQueries({
        queryKey: ["experiment-learnings", experimentId],
      });
    },
  });
}
