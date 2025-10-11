import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Experiment } from "./useExperiments";

export interface UpdateExperiment {
  name: string;
  hypothesis: string;
  kpiTarget: number;
  metricPresetId?: string;
  sampleSize?: number;
  mde?: number;
  startDate?: string;
  endDate?: string;
  creativesToGenerate?: number;
  instantFormsToGenerate?: number;
  emailsToGenerate?: number;
  journeyTemplateId?: number;
  facebookPageId?: number | null;
  facebookInstantFormId?: number | null;
  instagramAccountId?: number | null;
}

export function useUpdateExperiment(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: UpdateExperiment) => {
      const { data: experiment } = await axios.patch<Experiment>(
        `/api/experiments/${id}`,
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
