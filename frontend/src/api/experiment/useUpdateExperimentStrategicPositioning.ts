import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "./useExperiments";

export interface UpdateExperimentStrategicPositioning {
  commercialObjective: string | null;
  currentOperationalFunction: string | null;
}

export function useUpdateExperimentStrategicPositioning(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: UpdateExperimentStrategicPositioning) => {
      const { data: experiment } = await axios.patch<Experiment>(
        `/api/experiments/${id}/strategic-positioning`,
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
