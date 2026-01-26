import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

import type { Audience } from "./useAudiencesByNiche";

interface ReprocessAudienceTargetingSeedsPayload {
  id: number;
}

export function useReprocessAudienceTargetingSeeds(nicheId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id }: ReprocessAudienceTargetingSeedsPayload) => {
      const { data } = await axios.post<Audience>(
        `/api/audiences/${id}/targeting-seeds/reprocess`,
        null,
        {
          params: { includeTargeting: "true" },
        },
      );
      return data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ["audience", variables.id, true],
      });
      if (nicheId) {
        queryClient.invalidateQueries({
          queryKey: ["niche-audiences", nicheId, true],
        });
        queryClient.invalidateQueries({
          queryKey: ["niche-audiences", nicheId, false],
        });
      }
    },
  });
}
