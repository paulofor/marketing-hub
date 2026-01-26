import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { MarketNiche } from "./useNiches";

export interface RequestAudiencesPayload {
  quantity: number;
  model?: string;
}

export function useRequestAudiences(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ quantity, model }: RequestAudiencesPayload) => {
      const params: Record<string, string | number> = { quantity };
      if (model) {
        params.model = model;
      }
      const { data } = await axios.patch<MarketNiche>(
        `/api/niches/${id}/audiences-to-generate`,
        undefined,
        { params },
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
