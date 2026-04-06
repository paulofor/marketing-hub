import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import { Hypothesis } from "./useHypothesisBoard";
import type { HypothesisFramework } from "./types";

export interface UpdateHypothesisPayload {
  id: string;
  title: string;
  promise?: string;
  problem?: string;
  persona?: string;
  premiseAngleId?: number;
  mechanism?: string;
  uniqueMechanism?: string;
  entrega?: string;
  successRule?: string;
  imageFilterTitle?: string;
  prompt?: string;
  model?: string;
  cost?: number | null;
  expense?: number | null;
  offerType?: string;
  price?: number | null;
  kpiTargetCpl?: number;
  offerPackageId?: number | null;
  framework?: HypothesisFramework | null;
}

export function useUpdateHypothesis(nicheId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...body }: UpdateHypothesisPayload) => {
      const { data } = await axios.put<Hypothesis>(
        `/api/hypotheses/${id}`,
        body,
      );
      return data;
    },
    onSuccess: () => {
      if (nicheId) {
        qc.invalidateQueries({ queryKey: ["hypothesis-board", nicheId] });
      }
      qc.invalidateQueries({ queryKey: ["hypotheses"] });
      toast.success("Hipótese atualizada");
    },
    onError: () => {
      toast.error("Erro ao atualizar hipótese");
    },
  });
}
