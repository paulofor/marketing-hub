import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FacebookPageSummary {
  id: number;
  accountId: number;
  pageId: string;
  name: string;
}

export interface InstagramAccountSummary {
  id: number;
  handle: string;
  code: string;
  name: string;
}

export interface Experiment {
  id: string;
  nicheId: number;
  hypothesisId: string;
  name: string;
  hypothesis: string;
  pageId?: string | null;
  facebookPage?: FacebookPageSummary | null;
  instagramAccount?: InstagramAccountSummary | null;
  /**
   * KPI alvo em CPL. Mantém `kpiTarget` para compatibilidade com APIs
   * antigas que usavam este nome.
   */
  kpiTarget?: number;
  kpiTargetCpl?: number;
  stopLossCpl?: number | null;
  sampleSize?: number | null;
  baselineCvr?: number | null;
  targetCvr?: number | null;
  mdePercent?: number | null;
  startDate: string | null;
  endDate: string | null;
  metricPresetId?: string | null;
  creativesToGenerate?: number | null;
  creativeApproved: boolean;
  status: string;
  platform: string;
  createdAt: string;
  updatedAt: string;
  salesFunnelId?: string | null;
  salesFunnelName?: string | null;
}

export function useExperiments() {
  return useQuery({
    queryKey: ["experiments"],
    queryFn: async () => {
      const { data } = await axios.get<Experiment[]>("/api/experiments");
      return data;
    },
  });
}
