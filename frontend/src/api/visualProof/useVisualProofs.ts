import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface VisualProof {
  id: number;
  name: string;
  proofType?: string;
  description?: string;
}

export function useVisualProofs() {
  return useQuery({
    queryKey: ["visualProofs"],
    queryFn: async () => {
      const { data } = await axios.get<VisualProof[]>("/api/visual-proofs");
      return data;
    },
  });
}
