import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { InformationSource } from "./types";

export function useInformationSourcesByNiche(nicheId?: string | number) {
  return useQuery({
    queryKey: ["information-sources", "niche", nicheId],
    queryFn: async () => {
      const { data } = await axios.get<InformationSource[]>(
        `/api/niches/${nicheId}/information-sources`,
      );
      return data;
    },
    enabled: nicheId !== undefined && nicheId !== null && nicheId !== "",
  });
}
