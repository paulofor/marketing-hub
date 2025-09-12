import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { MarketNiche } from "./useNiches";

export function useRequestAudiences(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (quantity: number) => {
      const { data } = await axios.patch<MarketNiche>(
        `/api/niches/${id}/audiences-to-generate`,
        undefined,
        { params: { quantity } },
      );
      return data;
    },
    onSuccess: (data) => {
      // Update niche detail immediately and refetch related queries
      queryClient.setQueryData(["niche", id], data);
      queryClient.invalidateQueries({ queryKey: ["niches"] });
      queryClient.invalidateQueries({
        queryKey: ["niche-audiences", String(id)],
      });
    },
  });
}
