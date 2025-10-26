import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "./useExperiments";

export function useRequestDeliverables(id: string, nicheId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (quantity: number) => {
      const { data } = await axios.patch<Experiment>(
        `/api/experiments/${id}/deliverables-to-generate`,
        undefined,
        { params: { quantity } },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment", id] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
      if (nicheId != null) {
        queryClient.invalidateQueries({ queryKey: ["deliverables", "niche", nicheId] });
      }
    },
  });
}
