import { useMutation, useQuery } from "@tanstack/react-query";
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

export interface GeneratePromiseOptionsResponse {
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

export function usePromiseOptionsRequest(requestId?: number) {
  return useQuery({
    queryKey: ["experiment-promise-options-request", requestId],
    queryFn: async () => {
      const { data } = await axios.get<GeneratePromiseOptionsResponse>(
        `/api/experiments/promise-contract-options/stage-executions/${requestId}`,
      );
      return data;
    },
    enabled: Boolean(requestId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status && ["COMPLETED", "FAILED"].includes(status) ? false : 3000;
    },
  });
}
