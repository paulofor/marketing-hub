import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Audience {
  id: number;
  name: string;
  description: string;
  marketNicheId?: number;
  hypothesisId?: string;
  prompt?: string;
  model?: string;
  approved: boolean;
  targetingSpec?: string | null;
  targetingStatus?: string | null;
  targetingNotes?: string | null;
  seeds?: AudienceTargetingSeed[] | null;
}

export interface AudienceTargetingSeed {
  id: number;
  type: string;
  value: string;
  metaId?: string | null;
  key?: string | null;
  confidence?: number | null;
  status?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export function useAudiencesByNiche(
  nicheId?: string,
  options?: { includeTargeting?: boolean },
) {
  const includeTargeting = options?.includeTargeting ?? false;
  return useQuery({
    queryKey: ["niche-audiences", nicheId, includeTargeting],
    queryFn: async () => {
      if (!nicheId) return [] as Audience[];
      const { data } = await axios.get<Audience[]>(
        `/api/niches/${nicheId}/audiences`,
        {
          params: includeTargeting ? { includeTargeting: "true" } : undefined,
        },
      );
      return data;
    },
    enabled: !!nicheId,
  });
}
