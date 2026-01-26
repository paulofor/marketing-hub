import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

import type { Audience } from "./useAudiencesByNiche";

export interface UpdateAudienceTargetingPayload {
  id: number;
  targetingSpec?: string | null;
  status?: string | null;
  notes?: string | null;
  seeds?: Array<{
    type?: string | null;
    value?: string | null;
    metaId?: string | null;
    key?: string | null;
    confidence?: number | null;
    status?: string | null;
  }> | null;
}

export function useUpdateAudienceTargeting(nicheId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: UpdateAudienceTargetingPayload) => {
      const { id, ...body } = payload;
      const { data } = await axios.patch<Audience>(
        `/api/audiences/${id}/targeting`,
        body,
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
