import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentCampaignResetSummary {
  campaigns: number;
  adSets: number;
  ads: number;
  creatives: number;
}

export function useExperimentCampaignResetPreview(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-campaign-reset-preview", experimentId],
    enabled: false,
    queryFn: async () => {
      if (!experimentId) throw new Error("experiment id is required");
      const { data } = await axios.get<ExperimentCampaignResetSummary>(
        `/api/experiments/${experimentId}/facebook-campaigns/reset-preview`,
      );
      return data;
    },
  });
}

export function useExperimentCampaignReset(experimentId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      if (!experimentId) throw new Error("experiment id is required");
      const { data } = await axios.post<ExperimentCampaignResetSummary>(
        `/api/experiments/${experimentId}/facebook-campaigns/reset`,
      );
      return data;
    },
    onSuccess: () => {
      if (!experimentId) return;
      queryClient.invalidateQueries({
        queryKey: ["experiment-facebook-campaigns", experimentId],
      });
      queryClient.invalidateQueries({
        queryKey: ["experiment-diagnostics", experimentId],
      });
    },
  });
}
