import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { InstagramAccountSummary } from "./experiment/useExperiments";

export interface FacebookReadyExperiment {
  id: number;
  name: string;
  hypothesis: string;
  singlePain?: string | null;
  freeReward?: string | null;
  funnelPromise?: string | null;
  primaryCta?: string | null;
  experimentType?: string | null;
  campaignObjective?: string | null;
  followUpActionUrl?: string | null;
  kpiTargetCpl: number | null;
  startDate: string | null;
  endDate: string | null;
  nicheName: string | null;
  hypothesisTitle: string | null;
  missingConfiguration: string[];
  instagramAccount?: InstagramAccountSummary | null;
}

export function useFacebookReadyExperiments() {
  return useQuery({
    queryKey: ["facebook-experiments-ready"],
    queryFn: async () => {
      const { data } = await axios.get<FacebookReadyExperiment[]>(
        "/api/facebook-campaigns/experiments-ready",
      );
      return data;
    },
  });
}
