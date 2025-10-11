import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface DeleteExperimentEmailParams {
  experimentId: string;
  stepId: string;
  journeyId?: number | string;
}

export function useDeleteExperimentEmail({ experimentId, stepId, journeyId }: DeleteExperimentEmailParams) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await axios.delete(`/api/experiments/${experimentId}/emails/${stepId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment-email", experimentId, stepId] });
      if (journeyId != null) {
        queryClient.invalidateQueries({ queryKey: ["journeys", Number(journeyId)] });
      }
    },
  });
}
