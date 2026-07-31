import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentCockpitScoreboard {
  spend?: number | null;
  revenue?: number | null;
  margin?: number | null;
  roas?: number | null;
  impressions?: number | null;
  clicks?: number | null;
  ctr?: number | null;
  cpc?: number | null;
  pageViews: number;
  leads: number;
  checkoutAccesses: number;
  purchases: number;
  costPerLead?: number | null;
  costPerCheckoutAccess?: number | null;
  costPerPurchase?: number | null;
}

export interface ExperimentCockpitQuestion {
  pain?: string | null;
  promise?: string | null;
  mechanism?: string | null;
  offer?: string | null;
  primaryCta?: string | null;
  primaryVariable?: string | null;
  primaryMetric?: string | null;
}

export interface ExperimentCockpitHealth {
  status: string;
  headline: string;
  description: string;
  blockers: string[];
}

export interface ExperimentCockpitFunnelStage {
  stage: string;
  label: string;
  order: number;
  totalCount: number;
  uniqueCount?: number | null;
  lastEventAt?: string | null;
  source?: string | null;
}

export interface ExperimentCockpitBottleneck {
  code: string;
  title: string;
  severity: string;
  diagnosis: string;
  commercialImpact: string;
  recommendedFocus: string;
}

export interface ExperimentCockpitAction {
  code: string;
  label: string;
  rationale: string;
  targetRoute: string;
}

export interface ExperimentCockpit {
  experimentId: number;
  experimentName: string;
  status?: string | null;
  experimentType?: string | null;
  campaignObjective?: string | null;
  scoreboard: ExperimentCockpitScoreboard;
  question: ExperimentCockpitQuestion;
  health: ExperimentCockpitHealth;
  funnel: ExperimentCockpitFunnelStage[];
  bottleneck: ExperimentCockpitBottleneck;
  learnings: string[];
  nextActions: ExperimentCockpitAction[];
}

export function useExperimentCockpit(experimentId?: string) {
  return useQuery<ExperimentCockpit>({
    queryKey: ["experiment", experimentId, "cockpit"],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentCockpit>(
        `/api/experiments/${experimentId}/cockpit`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
  });
}
