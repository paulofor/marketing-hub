import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "../experiment/useExperiments";

interface Payload {
  sampleEmailId: number | null;
}

export function useSelectSampleEmail(experimentId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (sampleEmailId: number | null) => {
      const payload: Payload = { sampleEmailId };
      const { data } = await axios.put<Experiment>(
        `/api/experiments/${experimentId}/selected-sample-email`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sample-emails", experimentId] });
      queryClient.invalidateQueries({ queryKey: ["experiment", experimentId] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
      queryClient.invalidateQueries({ queryKey: ["lead-portal-experiment-metrics"] });
    },
  });
}
