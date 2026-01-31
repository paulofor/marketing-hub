import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingCandidate } from "./types";

export interface ReprocessTargetingCandidatePayload {
  candidateId: number;
  texto_sugerido?: string;
  idioma?: string;
  pais?: string;
}

export function useReprocessTargetingCandidate(limit = 10) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ candidateId, ...payload }: ReprocessTargetingCandidatePayload) => {
      const { data } = await axios.post<TargetingCandidate>(
        `/api/targeting/candidates/${candidateId}/reprocess`,
        Object.keys(payload).length > 0 ? payload : undefined,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["targeting-requests", limit] });
    },
  });
}
