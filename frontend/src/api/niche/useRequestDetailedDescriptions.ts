import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { MarketNiche } from "./useNiches";

export interface RequestDetailedDescriptionsPayload {
  quantity: number;
  model?: string;
}

export function useRequestDetailedDescriptions(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ quantity, model }: RequestDetailedDescriptionsPayload) => {
      const params: Record<string, string | number> = { quantity };
      if (model) params.model = model;
      const { data } = await axios.patch<MarketNiche>(
        `/api/niches/${id}/detailed-descriptions-to-generate`,
        undefined,
        { params },
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(["niche", id], data);
      queryClient.invalidateQueries({ queryKey: ["niches"] });
      queryClient.invalidateQueries({ queryKey: ["niche-descriptions", id] });
    },
  });
}
