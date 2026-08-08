import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { Hypothesis } from "./useHypothesisBoard";

export interface CreateHypothesisVersionPayload {
  sourceId: string;
  problem: string;
  persona: string;
  promise?: string;
  mechanism?: string;
  uniqueMechanism?: string;
  entrega: string;
  successRule?: string;
  offerType?: string;
  price: number;
}

export function useCreateHypothesisVersion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      sourceId,
      ...body
    }: CreateHypothesisVersionPayload) => {
      const { data } = await axios.post<Hypothesis>(
        `/api/hypotheses/${sourceId}/versions`,
        body,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["niche-hypotheses"] });
      queryClient.invalidateQueries({ queryKey: ["hypotheses"] });
      queryClient.invalidateQueries({ queryKey: ["hypothesis-board"] });
      toast.success("Nova versão da hipótese criada");
    },
    onError: () => toast.error("Não foi possível criar a nova versão"),
  });
}
