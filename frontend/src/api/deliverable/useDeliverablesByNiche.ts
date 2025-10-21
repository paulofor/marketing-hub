import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { Deliverable } from "./types";

export function useDeliverablesByNiche(nicheId?: string | number) {
  return useQuery({
    queryKey: ["deliverables", "niche", nicheId],
    queryFn: async () => {
      const { data } = await axios.get<Deliverable[]>(
        `/api/niches/${nicheId}/deliverables`,
      );
      return data;
    },
    enabled: nicheId !== undefined && nicheId !== null && nicheId !== "",
  });
}
