import { useMutation } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";

export interface PromiseOption {
  singlePain: string;
  freeReward: string;
  funnelPromise: string;
  primaryCta: string;
  reason: string;
}

interface GeneratePromiseOptionsPayload {
  nicheId: number;
  hypothesisId?: string;
  hypothesis?: string;
  currentSinglePain?: string;
  currentFreeReward?: string;
  currentFunnelPromise?: string;
  currentPrimaryCta?: string;
}

interface GeneratePromiseOptionsResponse {
  options: PromiseOption[];
}

export function useGeneratePromiseOptions() {
  return useMutation({
    mutationFn: async (payload: GeneratePromiseOptionsPayload) => {
      const { data } = await axios.post<GeneratePromiseOptionsResponse>(
        "/api/experiments/promise-contract-options/generate",
        payload,
      );
      return data.options;
    },
    onSuccess: () => {
      toast.success("IA gerou 3 opções de promessa");
    },
    onError: () => {
      toast.error("Não foi possível gerar opções com IA");
    },
  });
}
