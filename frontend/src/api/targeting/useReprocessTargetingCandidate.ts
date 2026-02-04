import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingCandidate } from "./types";
import {
  TARGETING_REQUESTS_QUERY_KEY,
  buildTargetingRequestsQueryKey,
  type TargetingRequestQueryFilters,
} from "./useTargetingRequests";

export interface ReprocessTargetingCandidatePayload {
  candidateId: number;
  texto_sugerido?: string;
  idioma?: string;
  pais?: string;
}

export function useReprocessTargetingCandidate(filters?: TargetingRequestQueryFilters) {
  const queryClient = useQueryClient();
  const queryKey = buildTargetingRequestsQueryKey(filters);

  return useMutation({
    mutationFn: async ({ candidateId, ...payload }: ReprocessTargetingCandidatePayload) => {
      const { data } = await axios.post<TargetingCandidate>(
        `/api/targeting/candidates/${candidateId}/reprocess`,
        Object.keys(payload).length > 0 ? payload : undefined,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      queryClient.invalidateQueries({ queryKey: [TARGETING_REQUESTS_QUERY_KEY] });
    },
  });
}
