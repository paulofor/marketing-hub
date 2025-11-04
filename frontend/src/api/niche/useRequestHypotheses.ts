import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { MarketNiche } from "./useNiches";

export function useRequestHypotheses(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (quantity: number) => {
      const { data } = await axios.patch<MarketNiche>(
        `/api/niches/${id}/hypotheses-to-generate`,
        undefined,
        { params: { quantity } },
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(["niche", id], data);
      queryClient.invalidateQueries({ queryKey: ["niches"] });
      queryClient.invalidateQueries({ queryKey: ["niche-hypotheses"] });
    },
  });
}
