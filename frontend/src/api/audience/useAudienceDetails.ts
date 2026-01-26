import { useQuery } from "@tanstack/react-query";
import axios from "axios";

import type { Audience } from "./useAudiencesByNiche";

export function useAudienceDetails(
  audienceId?: number,
  includeTargeting = false,
  enabled = true,
) {
  return useQuery({
    queryKey: ["audience", audienceId, includeTargeting],
    queryFn: async () => {
      if (!audienceId) return null;
      const { data } = await axios.get<Audience>(
        `/api/audiences/${audienceId}`,
        {
          params: includeTargeting ? { includeTargeting: "true" } : undefined,
        },
      );
      return data;
    },
    enabled: Boolean(audienceId) && enabled,
  });
}
