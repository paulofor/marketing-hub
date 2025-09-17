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
}

export function useAudiencesByNiche(nicheId?: string) {
  return useQuery({
    queryKey: ["niche-audiences", nicheId],
    queryFn: async () => {
      if (!nicheId) return [] as Audience[];
      const { data } = await axios.get<Audience[]>(
        `/api/niches/${nicheId}/audiences`
      );
      return data;
    },
    enabled: !!nicheId,
  });
}
