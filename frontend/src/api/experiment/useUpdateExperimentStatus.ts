import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "./useExperiments";

interface UpdateExperimentStatusInput {
  id: string;
  status: string;
}

interface ReactivateExperimentInput {
  id: string;
  reason: string;
}

export function useUpdateExperimentStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, status }: UpdateExperimentStatusInput) => {
      const { data } = await axios.patch<Experiment>(
        `/api/experiments/${id}/status`,
        null,
        {
          params: { status },
        },
      );
      return data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["experiment", variables.id] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
      queryClient.invalidateQueries({
        queryKey: ["experiment-readiness", variables.id],
      });
      queryClient.invalidateQueries({
        queryKey: ["experiment-runs", variables.id],
      });
    },
  });
}

export function useReactivateExperiment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, reason }: ReactivateExperimentInput) => {
      const { data } = await axios.post<Experiment>(
        `/api/experiments/${id}/reactivate`,
        {
          reason,
        },
      );
      return data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["experiment", variables.id] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
