import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Hypothesis } from "./useHypothesisBoard";

export function useUpdateHypothesisStatus(experimentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: { id: number; status: string }) => {
      const { data } = await axios.patch<Hypothesis>(
        `/api/hypotheses/${input.id}/status?status=${input.status}`,
      );
      return data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["hypothesis-board", experimentId] });
    },
  });
}
