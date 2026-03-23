import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { ExperimentFunnelStage } from "./useExperimentFunnel";

export interface RegisterExperimentFunnelEventPayload {
  stage: ExperimentFunnelStage;
  leadId?: string;
  source?: string;
  campaignCode?: string;
  payload?: string;
  occurredAt?: string;
}

export function useRegisterExperimentFunnelEvent(experimentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: RegisterExperimentFunnelEventPayload) => {
      await axios.post(`/api/experiments/${experimentId}/funnel/events`, input);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["experiment", experimentId, "funnel"],
      });
    },
  });
}
