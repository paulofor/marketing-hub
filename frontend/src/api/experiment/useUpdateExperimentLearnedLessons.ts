import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "./useExperiments";

export interface UpdateExperimentLearnedLessons {
  learnedLessons: string | null;
}

export function useUpdateExperimentLearnedLessons(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: UpdateExperimentLearnedLessons) => {
      const { data: experiment } = await axios.patch<Experiment>(
        `/api/experiments/${id}/learned-lessons`,
        data,
      );
      return experiment;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment", id] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
