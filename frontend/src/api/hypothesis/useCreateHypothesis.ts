import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import { Hypothesis } from "./useHypothesisBoard";

export interface CreateHypothesis {
  experimentId: number;
  title: string;
  premiseAngleId?: number;
  offerType?: string;
  kpiTargetCpl?: number;
}

export function useCreateHypothesis() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: CreateHypothesis) => {
      const { experimentId, ...body } = input;
      const { data } = await axios.post<Hypothesis>(
        `/api/experiments/${experimentId}/hypotheses`,
        body,
      );
      return data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: ["hypothesis-board", String(variables.experimentId)],
      });
      toast.success("Hipótese criada");
    },
    onError: () => {
      toast.error("Erro ao criar hipótese");
    },
  });
}
