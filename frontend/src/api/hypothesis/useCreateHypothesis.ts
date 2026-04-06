import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import { Hypothesis } from "./useHypothesisBoard";
import type { HypothesisFramework } from "./types";

export interface CreateHypothesis {
  marketNicheId: number;
  title: string;
  problem: string;
  premiseAngleId?: number;
  promise?: string;
  persona?: string;
  mechanism?: string;
  uniqueMechanism?: string;
  entrega?: string;
  successRule?: string;
  imageFilterTitle?: string;
  prompt?: string;
  model?: string;
  offerType?: string;
  kpiTargetCpl?: number;
  price?: number;
  offerPackageId?: number;
  costUsd?: number;
  cost?: number;
  expense?: number;
  framework?: HypothesisFramework | null;
}

export function useCreateHypothesis() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateHypothesis) => {
      const { marketNicheId, ...body } = input;
      const { data } = await axios.post<Hypothesis>(`/api/hypotheses`, {
        ...body,
        marketNicheId,
      });
      return data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: ["hypothesis-board", String(variables.marketNicheId)],
      });
      queryClient.invalidateQueries({ queryKey: ["niche-hypotheses"] });
      queryClient.invalidateQueries({ queryKey: ["hypotheses"] });
      toast.success("Hipótese criada");
    },
    onError: () => {
      toast.error("Erro ao criar hipótese");
    },
  });
}
