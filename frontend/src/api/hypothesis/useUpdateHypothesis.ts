import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import { Hypothesis } from "./useHypothesisBoard";

export function useUpdateHypothesis(nicheId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (h: Hypothesis) => {
      const { id, ...body } = h;
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
