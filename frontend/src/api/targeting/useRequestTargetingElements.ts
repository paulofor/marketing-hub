import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { MarketNiche } from "../niche/useNiches";
import type { TargetingElementType } from "./types";

export interface RequestTargetingElementsPayload {
  quantity: number;
  model?: string;
}

const ENDPOINT_BY_TYPE: Record<TargetingElementType, string> = {
  INTEREST: "interests-to-generate",
  JOB_TITLE: "job-titles-to-generate",
  BEHAVIOR: "behaviors-to-generate",
};

export function useRequestTargetingElements(
  nicheId: number,
  type: TargetingElementType,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ quantity, model }: RequestTargetingElementsPayload) => {
      const params: Record<string, string | number> = { quantity };
      if (model) {
        params.model = model;
      }
      const endpoint = ENDPOINT_BY_TYPE[type];
      const { data } = await axios.patch<MarketNiche>(
        `/api/niches/${nicheId}/${endpoint}`,
        undefined,
        { params },
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(["niche", nicheId], data);
      queryClient.invalidateQueries({ queryKey: ["niches"] });
      queryClient.invalidateQueries({ queryKey: ["niche-targeting-elements"] });
    },
  });
}
