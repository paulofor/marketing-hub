import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "../experiment/useExperiments";

export function useRequestSampleEmails(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (quantity: number) => {
      const { data } = await axios.patch<Experiment>(
        `/api/experiments/${id}/sample-emails-to-generate`,
        undefined,
        { params: { quantity } },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment", id] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
      queryClient.invalidateQueries({ queryKey: ["sample-emails", id] });
    },
  });
}
