import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentFacebookAdDto {
  id: string;
  name: string;
  status: string;
  createdAt?: string | null;
}

export interface ExperimentFacebookAdSetDto {
  id: string;
  name: string;
  status: string;
  createdAt?: string | null;
  experimentAdSetId?: number | null;
  ads: ExperimentFacebookAdDto[];
  issues: string[];
}

export interface ExperimentFacebookCampaignDto {
  id: string;
  name: string;
  objective: string;
  status: string;
  createdAt?: string | null;
  updatedAt?: string | null;
  metricsLastSyncedAt?: string | null;
  metricsLastError?: string | null;
  adSets: ExperimentFacebookAdSetDto[];
  issues: string[];
}

export function useExperimentFacebookCampaigns(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-facebook-campaigns", experimentId],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<ExperimentFacebookCampaignDto[]>(
        `/api/experiments/${experimentId}/facebook-campaigns`,
      );
      return data;
    },
  });
}
