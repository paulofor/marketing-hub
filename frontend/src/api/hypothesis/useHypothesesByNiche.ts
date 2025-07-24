import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { Hypothesis } from "./useHypothesisBoard";

export function useHypothesesByNiche(nicheId?: string, status: string = "BACKLOG") {
  return useQuery({
    queryKey: ["niche-hypotheses", nicheId, status],
    queryFn: async () => {
      if (!nicheId) return [] as Hypothesis[];
      const { data } = await axios.get<Hypothesis[]>(
        `/api/niches/${nicheId}/hypotheses?status=${status}`,
      );
      return data;
    },
    enabled: !!nicheId,
  });
}
