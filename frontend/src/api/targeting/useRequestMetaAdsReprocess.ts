import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface RequestMetaAdsReprocessPayload {
  id: number;
  nicheId?: number;
}

export function useRequestMetaAdsReprocess() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id }: RequestMetaAdsReprocessPayload) => {
      const { data } = await axios.post(`/api/targeting-elements/${id}/metaads/reprocess`);
      return data;
    },
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({ queryKey: ["niche-targeting-elements"] });
      await queryClient.invalidateQueries({ queryKey: ["targeting-recent-requests"] });
      if (variables.nicheId) {
        await queryClient.invalidateQueries({ queryKey: ["niche", variables.nicheId] });
      }
    },
  });
}
