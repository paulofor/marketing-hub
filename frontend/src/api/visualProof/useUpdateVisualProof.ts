import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { VisualProof } from "./useVisualProofs";

export interface UpdateVisualProof {
  id: number;
  name: string;
  proofType?: string;
  description?: string;
}

export function useUpdateVisualProof() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (data: UpdateVisualProof) => {
      const { data: proof } = await axios.put<VisualProof>(
        `/api/visual-proofs/${data.id}`,
        data,
      );
      return proof;
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["visualProofs"] });
    },
  });
}
