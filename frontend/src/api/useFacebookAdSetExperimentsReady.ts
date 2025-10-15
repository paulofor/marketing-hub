import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ReadyAudience {
  id: number;
  name: string;
  description?: string | null;
  prompt?: string | null;
  model?: string | null;
  approved: boolean;
}

export interface ReadyHypothesis {
  id: string;
  title: string;
  promise?: string | null;
  persona?: string | null;
  mechanism?: string | null;
  uniqueMechanism?: string | null;
}

export interface ReadyNiche {
  id: number;
  name: string;
  description?: string | null;
  baseSegmentation?: string | null;
  interests?: string | null;
  demographicFilters?: string | null;
  extraTips?: string | null;
}

export interface ReadyExperiment {
  id: number;
  name: string;
  hypothesis?: string | null;
  status?: string | null;
  platform?: string | null;
  kpiTargetCpl?: number | string | null;
  startDate?: string | null;
  endDate?: string | null;
  creativeApproved?: boolean;
  facebookInstantForm?: {
    id: number;
    facebookFormId: string | null;
    name: string;
  } | null;
  facebookPage?: {
    id: number;
    pageId: string;
    name: string;
  } | null;
}

export interface ExperimentReadyForAdSet {
  experiment: ReadyExperiment;
  niche: ReadyNiche | null;
  hypothesis: ReadyHypothesis | null;
  audiences: ReadyAudience[];
}

export function useFacebookAdSetExperimentsReady() {
  return useQuery({
    queryKey: ["facebook-adsets-experiments-ready"],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentReadyForAdSet[]>(
        "/api/facebook-adsets/experiments-ready",
      );
      return data;
    },
    staleTime: 1000 * 30,
  });
}

