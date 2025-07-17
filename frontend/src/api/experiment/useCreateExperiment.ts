import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Experiment } from "./useExperiments";

export interface CreateExperiment {
  hypothesis: string;
  kpiGoal: number;
  startDate: string;
  endDate: string;
}

export function useCreateExperiment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateExperiment) => {
      const { data: experiment } = await axios.post<Experiment>(
        "/api/experiments",
        data,
      );
      return experiment;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
