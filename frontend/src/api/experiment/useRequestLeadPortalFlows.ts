import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Experiment } from "./useExperiments";

export function useRequestLeadPortalFlows(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (quantity: number) => {
      const { data } = await axios.patch<Experiment>(
        `/api/experiments/${id}/lead-portal-flows-to-generate`,
        undefined,
        { params: { quantity } },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment", id] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
