import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ProofArtifact } from "./types";

export function useHypothesisProofs(hypothesisId?: string) {
  return useQuery({
    queryKey: ["proofs", "hypothesis", hypothesisId],
    queryFn: async () => {
      const { data } = await axios.get<ProofArtifact[]>(
        `/api/hypotheses/${hypothesisId}/proofs`,
      );
      return data;
    },
    enabled: Boolean(hypothesisId),
  });
}
