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

export interface FacebookInstantFormSummary {
  id: number;
  hypothesisId: string;
  facebookPageId: number;
  facebookPageExternalId: string;
  facebookPageName: string;
  facebookFormId: string;
  name: string;
  status?: string | null;
  locale?: string | null;
  leadsCount?: number | null;
  createdTime?: string | null;
  updatedTime?: string | null;
  followUpActionUrl?: string | null;
  privacyPolicyUrl?: string | null;
  model?: string | null;
  prompt?: string | null;
  approved?: boolean;
  approvedAt?: string | null;
}

export interface Experiment {
  id: string;
  nicheId: number;
  hypothesisId: string;
  name: string;
  hypothesis: string;
  pageId?: string | null;
  facebookPage?: FacebookPageSummary | null;
  facebookInstantForm?: FacebookInstantFormSummary | null;
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
  instantFormsToGenerate?: number | null;
  emailsToGenerate?: number | null;
  creativeApproved: boolean;
  status: string;
  platform: string;
  createdAt: string;
  updatedAt: string;
  journeyTemplateId?: number | null;
  journeyTemplateName?: string | null;
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
