import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { ProofArtifact, CreateProofPayload } from "./types";

export function useCreateHypothesisProof(hypothesisId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateProofPayload) => {
      const { data } = await axios.post<ProofArtifact>(
        `/api/hypotheses/${hypothesisId}/proofs`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["proofs", "hypothesis", hypothesisId],
      });
    },
  });
}
