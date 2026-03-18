import { useQuery } from "@tanstack/react-query";
import axios from "axios";

import type { LeadPortalSimpleFormStyle } from "./useLeadPortalSimpleFormStyles";

export interface LeadPortalFlowQuestion {
  id: number;
  title: string;
  dataKey: string;
  type: string;
  required: boolean;
  description?: string | null;
  placeholder?: string | null;
  position: number;
  options: string[];
}

export interface LeadPortalFlow {
  id: number;
  name: string;
  slug: string;
  marketNicheId?: number | null;
  experimentId?: number | null;
  publicUrl?: string | null;
  description?: string | null;
  customFormHtml?: string | null;
  model?: string | null;
  prompt?: string | null;
  imagePromptModel?: string | null;
  imagePromptTemplate?: string | null;
  imagePromptBatchSize?: number | null;
  approved: boolean;
  simpleFormStyle?: LeadPortalSimpleFormStyle | null;
  approvedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  questions: LeadPortalFlowQuestion[];
}

interface UseLeadPortalFlowsParams {
  experimentId?: number | string;
  nicheId?: number | string;
}

export function useLeadPortalFlows({
  experimentId,
  nicheId,
}: UseLeadPortalFlowsParams = {}) {
  return useQuery({
    queryKey: ["lead-portal-flows", experimentId ?? null, nicheId ?? null],
    queryFn: async () => {
      const params: Record<string, number | string> = {};
      if (experimentId) params.experimentId = experimentId;
      if (nicheId) params.nicheId = nicheId;
      const { data } = await axios.get<LeadPortalFlow[]>(
        "/api/lead-portal-flows",
        { params: Object.keys(params).length ? params : undefined },
      );
      return data;
    },
  });
}
