import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { VisualProof } from "./useVisualProofs";

export interface CreateVisualProof {
  name: string;
  proofType?: string;
  description?: string;
}

export function useCreateVisualProof() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateVisualProof) => {
      const { data: proof } = await axios.post<VisualProof>(
        "/api/visual-proofs",
        data,
      );
      return proof;
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["visualProofs"] });
    },
  });
}
