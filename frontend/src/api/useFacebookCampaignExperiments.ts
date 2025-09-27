import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentSummary {
  id: number;
  name: string;
  hypothesis: string;
  kpiTargetCpl: number | null;
  startDate: string | null;
  endDate: string | null;
  nicheName: string | null;
  hypothesisTitle: string | null;
  missingConfiguration: string[];
}

export function useFacebookCampaignExperiments(status: string) {
  return useQuery({
    queryKey: ["facebook-campaign-experiments", status],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentSummary[]>(
        "/api/facebook-campaigns/experiments",
        { params: { status } },
      );
      return data;
    },
  });
}
