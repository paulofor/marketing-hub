import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingRequest, TargetingRequestStatus } from "./types";

export interface TargetingRequestQueryFilters {
  limit?: number;
  status?: TargetingRequestStatus;
  nicheId?: number;
  hypothesisId?: string;
}

export const TARGETING_REQUESTS_QUERY_KEY = "targeting-requests" as const;

export function buildTargetingRequestsQueryKey(filters: TargetingRequestQueryFilters = {}) {
  const limit = filters.limit ?? 10;
  const status = filters.status ?? null;
  const nicheId = filters.nicheId ?? null;
  const hypothesisId = filters.hypothesisId ?? null;
  return [TARGETING_REQUESTS_QUERY_KEY, limit, status, nicheId, hypothesisId] as const;
}

export function useTargetingRequests(filters: TargetingRequestQueryFilters = {}) {
  const limit = filters.limit ?? 10;
  const queryKey = buildTargetingRequestsQueryKey(filters);

  return useQuery({
    queryKey,
    queryFn: async () => {
      const params: Record<string, unknown> = {
        limit,
        includeCandidates: true,
      };
      if (filters.status) {
        params.status = filters.status;
      }
      if (filters.nicheId) {
        params.nicheId = filters.nicheId;
      }
      if (filters.hypothesisId) {
        params.hypothesisId = filters.hypothesisId;
      }
      const { data } = await axios.get<TargetingRequest[]>("/api/targeting/requests", {
        params,
      });
      return data;
    },
    staleTime: 30_000,
  });
}
