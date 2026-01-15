import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { MarketNiche } from "./useNiches";

export interface RequestHypothesesPayload {
  quantity: number;
  model?: string;
  differentiatedTechnologyId?: number;
  detailedDescriptionId?: number;
}

export function useRequestHypotheses(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      quantity,
      model,
      differentiatedTechnologyId,
      detailedDescriptionId,
    }: RequestHypothesesPayload) => {
      const params: Record<string, string | number> = { quantity };
      if (model) params.model = model;
      if (differentiatedTechnologyId != null) {
        params.differentiatedTechnologyId = differentiatedTechnologyId;
      }
      if (detailedDescriptionId != null) {
        params.detailedDescriptionId = detailedDescriptionId;
      }
      const { data } = await axios.patch<MarketNiche>(
        `/api/niches/${id}/hypotheses-to-generate`,
        undefined,
        { params },
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
