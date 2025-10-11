import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface UpdateExperimentEmailApprovalParams {
  experimentId: string;
  stepId: string;
  journeyId?: number | string;
}

export function useUpdateExperimentEmailApproval({
  experimentId,
  stepId,
  journeyId,
}: UpdateExperimentEmailApprovalParams) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (approved: boolean) => {
      const { data } = await axios.patch(
        `/api/experiments/${experimentId}/emails/${stepId}/approval`,
        { approved },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment-email", experimentId, stepId] });
      if (journeyId != null) {
        queryClient.invalidateQueries({ queryKey: ["journeys", Number(journeyId)] });
      }
    },
  });
}
