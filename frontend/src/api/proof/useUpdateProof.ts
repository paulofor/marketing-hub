import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { ProofArtifact, UpdateProofPayload } from "./types";

export function useUpdateProof(hypothesisId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...payload }: UpdateProofPayload) => {
      const { data } = await axios.put<ProofArtifact>(`/api/proofs/${id}`, payload);
      return data;
    },
    onSuccess: () => {
      if (hypothesisId) {
        queryClient.invalidateQueries({
          queryKey: ["proofs", "hypothesis", hypothesisId],
        });
      }
    },
  });
}
