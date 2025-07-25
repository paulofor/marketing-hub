import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { toast } from "react-toastify";
import { Hypothesis } from "./useHypothesisBoard";

export function useUpdateHypothesisStatus(nicheId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: { id: number; status: string }) => {
      const { data } = await axios.patch<Hypothesis>(
        `/api/hypotheses/${input.id}/status?status=${input.status}`,
      );
      return data;
    },
    onSuccess: () => {
      if (nicheId) {
        qc.invalidateQueries({ queryKey: ["hypothesis-board", nicheId] });
      }
      qc.invalidateQueries({ queryKey: ["hypotheses"] });
      toast.success("Status atualizado");
    },
    onError: () => {
      toast.error("Erro ao atualizar status");
    },
  });
}
