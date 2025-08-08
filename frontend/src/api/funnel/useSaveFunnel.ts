import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface SaveFunnelStep {
  id?: string;
  stimulusType: string;
  expectedAction: string;
  scoreInc: number;
}

export interface SaveFunnel {
  id?: string;
  name: string;
  steps: SaveFunnelStep[];
}

export function useSaveFunnel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: SaveFunnel) => {
      const { data: funnel } = await axios.post(`/api/funnels`, data);
      return funnel;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["funnels"] });
    },
  });
}
