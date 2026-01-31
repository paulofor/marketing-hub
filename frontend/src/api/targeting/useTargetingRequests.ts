import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingRequest } from "./types";

export function useTargetingRequests(limit = 10) {
  return useQuery({
    queryKey: ["targeting-requests", limit],
    queryFn: async () => {
      const { data } = await axios.get<TargetingRequest[]>("/api/targeting/requests", {
        params: {
          limit,
          includeCandidates: true,
        },
      });
      return data;
    },
    staleTime: 30_000,
  });
}
