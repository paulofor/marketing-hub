import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "./useExperiments";

export function useUpdateExperimentStatus(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (status: string) => {
      const { data } = await axios.patch<Experiment>(`/api/experiments/${id}/status`, null, {
        params: { status },
      });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment", id] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
