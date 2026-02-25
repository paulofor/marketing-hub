import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingCandidateType, TargetingRequest } from "../targeting/types";

export interface ExperimentTargetingSelection {
  id: number;
  experimentId: number;
  candidateType: TargetingCandidateType;
  term: string;
}

export interface SaveExperimentTargetingSelectionsPayload {
  items: Array<{
    candidateType: TargetingCandidateType;
    term: string;
  }>;
}

export function useExperimentTargetingSelections(experimentId?: number) {
  return useQuery({
    queryKey: ["experiment-targeting-selections", experimentId],
    enabled: !!experimentId,
    queryFn: async () => {
      const { data } = await axios.get<ExperimentTargetingSelection[]>(
        `/api/experiments/${experimentId}/targeting-selections`,
      );
      return data;
    },
  });
}

export function useSaveExperimentTargetingSelections(experimentId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: SaveExperimentTargetingSelectionsPayload) => {
      const { data } = await axios.put<ExperimentTargetingSelection[]>(
        `/api/experiments/${experimentId}/targeting-selections`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment-targeting-selections", experimentId] });
    },
  });
}

export function useRunSimpleAudienceFlow(experimentId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const { data } = await axios.post<TargetingRequest>(
        `/api/experiments/${experimentId}/targeting-selections/run-simple-flow`,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment-targeting-selections", experimentId] });
    },
  });
}
