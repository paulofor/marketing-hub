import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "./useExperiments";

export function useExperimentsByNiche(nicheId?: string) {
  return useQuery({
    queryKey: ["niche-experiments", nicheId],
    queryFn: async () => {
      if (!nicheId) return [] as Experiment[];
      const { data } = await axios.get<Experiment[]>(
        `/api/niches/${nicheId}/experiments`,
      );
      return data;
    },
    enabled: !!nicheId,
  });
}
