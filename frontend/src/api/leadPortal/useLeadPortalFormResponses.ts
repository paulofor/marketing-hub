import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface LeadPortalFormResponseAnswer {
  key: string;
  value: string;
}

export interface LeadPortalFormResponse {
  id: string;
  flowSlug?: string | null;
  flowName?: string | null;
  experimentId?: number | null;
  experimentName?: string | null;
  name?: string | null;
  email?: string | null;
  phone?: string | null;
  submittedAt: string;
  answers: LeadPortalFormResponseAnswer[];
}

export function useLeadPortalFormResponses(limit = 50) {
  return useQuery<LeadPortalFormResponse[], Error>({
    queryKey: ["lead-portal-form-responses", limit],
    queryFn: async () => {
      const { data } = await axios.get<LeadPortalFormResponse[]>(
        "/api/lead-portal/form-responses",
        { params: { limit } },
      );
      return data;
    },
    staleTime: 15_000,
  });
}
