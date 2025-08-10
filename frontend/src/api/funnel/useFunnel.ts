import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FunnelStep {
  id: string;
  stimulusType: string;
  expectedAction: string;
  scoreInc: number;
  orderIdx: number;
  note?: string;
}

export interface FunnelDetail {
  id: string;
  name: string;
  objective?: string;
  steps: FunnelStep[];
}

export function useFunnel(id: string) {
  return useQuery<FunnelDetail>({
    queryKey: ["funnels", id],
    queryFn: async () => {
      const { data } = await axios.get<FunnelDetail>(`/api/funnels/${id}`);
      return data;
    },
    enabled: !!id,
  });
}
