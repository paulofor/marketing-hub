import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Experiment } from "./useExperiments";

export interface CreateExperiment {
  nicheId: number;
  name: string;
  hypothesis: string;
  kpiTarget: number;
  startDate?: string;
  endDate?: string;
}

export function useCreateExperiment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateExperiment) => {
      const { nicheId, ...payload } = data;
      const { data: experiment } = await axios.post<Experiment>(
        `/api/niches/${nicheId}/experiments`,
        payload,
      );
      return experiment;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
