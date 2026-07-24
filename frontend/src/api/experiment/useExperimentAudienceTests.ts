import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingCandidateType } from "../targeting/types";

export type ExperimentAudienceTestStatus =
  | "DRAFT"
  | "READY"
  | "RUNNING"
  | "PAUSED"
  | "WINNER"
  | "LOSER";

export interface ExperimentAudienceTestItem {
  id: number;
  candidateType: TargetingCandidateType;
  term: string;
  targetingElementId: number;
  metaId?: string | null;
  metaKey?: string | null;
  metaAudienceSizeLowerBound?: number | null;
  metaAudienceSizeUpperBound?: number | null;
}

export interface ExperimentAudienceTest {
  id: number;
  experimentId: number;
  name: string;
  hypothesis: string;
  successMetric: string;
  dailyBudget?: number | null;
  status: ExperimentAudienceTestStatus;
  audienceSizeLowerBound: number;
  audienceSizeUpperBound: number;
  createdAt?: string | null;
  updatedAt?: string | null;
  items: ExperimentAudienceTestItem[];
}

export interface CreateExperimentAudienceTestPayload {
  name: string;
  hypothesis: string;
  successMetric: string;
  dailyBudget?: number | null;
  items: Array<{
    candidateType: TargetingCandidateType;
    targetingElementId: number;
  }>;
}

export function useExperimentAudienceTests(experimentId?: number) {
  return useQuery({
    queryKey: ["experiment-audience-tests", experimentId],
    enabled: !!experimentId,
    queryFn: async () => {
      const { data } = await axios.get<ExperimentAudienceTest[]>(
        `/api/experiments/${experimentId}/audience-tests`,
      );
      return data;
    },
  });
}

export function useCreateExperimentAudienceTest(experimentId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateExperimentAudienceTestPayload) => {
      const { data } = await axios.post<ExperimentAudienceTest>(
        `/api/experiments/${experimentId}/audience-tests`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["experiment-audience-tests", experimentId],
      });
    },
  });
}

export function useDeleteExperimentAudienceTest(experimentId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (audienceTestId: number) => {
      await axios.delete(
        `/api/experiments/${experimentId}/audience-tests/${audienceTestId}`,
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["experiment-audience-tests", experimentId],
      });
    },
  });
}
