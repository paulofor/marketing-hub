import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface NicheDetailedDescription {
  id: number;
  marketNicheId: number;
  title?: string;
  description?: string;
  pains?: string | null;
  desires?: string | null;
  needs?: string | null;
  prompt?: string;
  model?: string;
  costUsd?: number | null;
  inputTokens?: number | null;
  outputTokens?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

export function useNicheDetailedDescriptions(nicheId?: string | number) {
  return useQuery({
    queryKey: ["niche-descriptions", nicheId],
    queryFn: async () => {
      if (nicheId === undefined || nicheId === null || nicheId === "") return [] as NicheDetailedDescription[];
      const { data } = await axios.get<NicheDetailedDescription[]>(
        `/api/niches/${nicheId}/descriptions`,
      );
      return data;
    },
    enabled: nicheId !== undefined && nicheId !== null && nicheId !== "",
  });
}
