import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { ExperimentJourneyAssignments } from "./useExperimentJourneyAssignments";

export function useRebuildExperimentJourney(experimentId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const { data } = await axios.post<ExperimentJourneyAssignments>(
        `/api/experiments/${experimentId}/journey/rebuild`,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(
        ["experiment-journey-assignments", experimentId],
        data,
      );
    },
  });
}
