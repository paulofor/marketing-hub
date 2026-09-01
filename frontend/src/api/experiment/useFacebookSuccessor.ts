import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Experiment } from "./useExperiments";

export interface FacebookSuccessorReadiness {
  available: boolean;
  existingSuccessorId?: number | null;
  blockers: string[];
}

export interface CreateFacebookSuccessorRequest {
  dailyBudget: number;
  mediaSpendLimit: number;
  startDate: string;
  endDate: string;
  facebookPageId: number;
  instagramAccountId: number;
}

export function useFacebookSuccessorReadiness(experimentId?: string | number) {
  return useQuery({
    queryKey: ["experiment-facebook-successor-readiness", experimentId],
    queryFn: async () => {
      const { data } = await axios.get<FacebookSuccessorReadiness>(
        `/api/experiments/${experimentId}/facebook-successor-readiness`,
      );
      return data;
    },
    enabled: experimentId !== undefined && experimentId !== null,
  });
}

export function useCreateFacebookSuccessor(sourceExperimentId: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (request: CreateFacebookSuccessorRequest) => {
      const { data } = await axios.post<Experiment>(
        `/api/experiments/${sourceExperimentId}/facebook-successors`,
        request,
      );
      return data;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["experiments"] }),
        queryClient.invalidateQueries({
          queryKey: [
            "experiment-facebook-successor-readiness",
            sourceExperimentId,
          ],
        }),
      ]);
    },
  });
}
