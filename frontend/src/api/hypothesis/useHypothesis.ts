import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { Hypothesis } from "./useHypothesisBoard";

export function useHypothesis(nicheId?: string, id?: string) {
  return useQuery({
    queryKey: ["hypothesis", nicheId, id],
    queryFn: async () => {
      if (!nicheId || !id) return undefined;
      const { data } = await axios.get<Hypothesis[]>(
        `/api/niches/${nicheId}/hypotheses`,
      );
      return data.find((h) => h.id === Number(id));
    },
    enabled: !!nicheId && !!id,
  });
}
