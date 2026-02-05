import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingRecentRequest } from "./types";

export const TARGETING_RECENT_REQUESTS_QUERY_KEY =
  "targeting-recent-requests" as const;

export function useTargetingRecentRequests(limit = 10) {
  const normalizedLimit = limit > 0 ? limit : 10;
  return useQuery({
    queryKey: [TARGETING_RECENT_REQUESTS_QUERY_KEY, normalizedLimit],
    queryFn: async () => {
      const { data } = await axios.get<TargetingRecentRequest[]>(
        "/api/targeting/requests/recent",
        {
          params: { limit: normalizedLimit },
        },
      );
      return data;
    },
    staleTime: 30_000,
  });
}
