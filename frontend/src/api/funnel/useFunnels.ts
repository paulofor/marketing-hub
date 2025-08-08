import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FunnelSummary {
  id: string;
  name: string;
  objective?: string;
  experimentCount: number;
}

export function useFunnels() {
  return useQuery<FunnelSummary[]>({
    queryKey: ["funnels"],
    queryFn: async () => {
      const { data } = await axios.get<FunnelSummary[]>("/api/funnels");
      return data;
    },
  });
}
