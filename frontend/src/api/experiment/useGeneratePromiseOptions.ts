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
  hypothesisId: string;
  currentSinglePain?: string;
  currentFreeReward?: string;
  currentFunnelPromise?: string;
  currentPrimaryCta?: string;
}

interface GeneratePromiseOptionsResponse {
  requestId: number;
  status: string;
  options: PromiseOption[];
}

export function useGeneratePromiseOptions() {
  return useMutation({
    mutationFn: async (payload: GeneratePromiseOptionsPayload) => {
      const { data } = await axios.post<GeneratePromiseOptionsResponse>(
        "/api/experiments/promise-contract-options/generate",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      toast.success("Solicitação registrada. O AI Worker vai gerar as opções.");
    },
    onError: () => {
      toast.error("Não foi possível gerar opções com IA");
    },
  });
}
