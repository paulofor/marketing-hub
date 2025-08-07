import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface CreateFunnelStep {
  stimulusType: string;
  expectedAction: string;
  scoreInc: number;
}

export interface CreateFunnel {
  name: string;
  steps: CreateFunnelStep[];
}

export function useCreateFunnel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateFunnel) => {
      const { data: funnel } = await axios.post("/api/funnels", data);
      return funnel;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["funnels"] });
    },
  });
}
