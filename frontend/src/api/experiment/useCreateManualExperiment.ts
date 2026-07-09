import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import type { Experiment } from "./useExperiments";

export interface CreateManualExperiment {
  nicheName: string;
  nicheAudience?: string;
  nicheDescription?: string;
  marketReference?: string;
  pains?: string;
  desires?: string;
  likelyChannels?: string;
  hypothesisStatement?: string;
  persona: string;
  problem: string;
  promise: string;
  mechanism: string;
  proof?: string;
  successSignal?: string;
  offerName?: string;
  leadMagnet: string;
  productName?: string;
  primaryCta: string;
  testPrice?: number;
  promiseLimit?: string;
  validationType?: string;
  experimentChannel?: string;
  dailyBudget?: number;
  kpiTargetCpl?: number;
  sampleSize?: number;
  creativeAngles?: string;
  successCriteria?: string;
  discardCriteria?: string;
}

export function useCreateManualExperiment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateManualExperiment) => {
      const { data } = await axios.post<Experiment>(
        "/api/manual-experiments",
        payload,
      );
      return data;
    },
    onSuccess: (experiment) => {
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
      queryClient.invalidateQueries({ queryKey: ["niches"] });
      queryClient.invalidateQueries({ queryKey: ["niche-summary"] });
      queryClient.invalidateQueries({ queryKey: ["hypotheses"] });
      toast.success(`Experimento manual criado: ${experiment.name}`);
    },
    onError: () => {
      toast.error("Não foi possível criar o experimento manual.");
    },
  });
}
