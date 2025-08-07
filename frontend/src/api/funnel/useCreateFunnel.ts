import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface CreateFunnelStep {
  stimulusType: string;
  expectedAction: string;
  scoreInc: number;
}

export interface CreateFunnel {
  experimentId: number;
  name: string;
  steps: CreateFunnelStep[];
}

export function useCreateFunnel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateFunnel) => {
      const { experimentId, ...body } = data;
      const { data: funnel } = await axios.post(
        `/api/funnels?experimentId=${experimentId}`,
        body,
      );
      return funnel;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["funnels"] });
    },
  });
}
