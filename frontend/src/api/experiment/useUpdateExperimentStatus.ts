import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "./useExperiments";

interface UpdateExperimentStatusInput {
  id: string;
  status: string;
}

export function useUpdateExperimentStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, status }: UpdateExperimentStatusInput) => {
      const { data } = await axios.patch<Experiment>(`/api/experiments/${id}/status`, null, {
        params: { status },
      });
      return data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["experiment", variables.id] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
