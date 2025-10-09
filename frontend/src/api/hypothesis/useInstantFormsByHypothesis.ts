import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface InstantForm {
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
  createdAt?: string | null;
  updatedAt?: string | null;
}

export function useInstantFormsByHypothesis(hypothesisId?: string) {
  return useQuery({
    queryKey: ["instant-forms", hypothesisId],
    enabled: Boolean(hypothesisId),
    queryFn: async () => {
      if (!hypothesisId) return [] as InstantForm[];
      const { data } = await axios.get<InstantForm[]>(
        `/api/hypotheses/${hypothesisId}/instant-forms`,
      );
      return data;
    },
  });
}
