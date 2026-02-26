import { useQuery } from "@tanstack/react-query";
import axios from "axios";

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
  publicUrl?: string | null;
  description?: string | null;
  model?: string | null;
  prompt?: string | null;
  approved: boolean;
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
