import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentAdSetJob {
  id: number;
  type: string;
  worker: string;
  status: string;
  attemptCount: number;
  lockedBy?: string | null;
  lockedAt?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  errorMessage?: string | null;
  resourceId?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface ExperimentAdSetSpec {
  id: number;
  slot: string;
  label?: string | null;
  ageMin?: number | null;
  ageMax?: number | null;
  targetingSpec?: string | null;
  validationStatus?: string | null;
  reachStatus?: string | null;
  reachLowerBound?: number | null;
  reachUpperBound?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface ExperimentAdSetWorkflowDto {
  workflowId: number;
  experimentId: number;
  status: string;
  seedKeyword?: string | null;
  seedLocale?: string | null;
  seedInterestId?: string | null;
  seedInterestName?: string | null;
  seedAudienceLower?: number | null;
  seedAudienceUpper?: number | null;
  aiNotes?: string | null;
  lastError?: string | null;
  completedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  jobs: ExperimentAdSetJob[];
  specs: ExperimentAdSetSpec[];
}

export function useExperimentAdSetWorkflow(experimentId?: string) {
  return useQuery<ExperimentAdSetWorkflowDto>({
    queryKey: ["experiment-adset-workflow", experimentId],
    queryFn: async () => {
      if (!experimentId) throw new Error("experimentId é obrigatório");
      const { data } = await axios.get<ExperimentAdSetWorkflowDto>(
        `/api/experiments/${experimentId}/adset-playbook`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
    refetchInterval: 30000,
  });
}

export function useStartExperimentAdSetWorkflow(experimentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (restart: boolean) => {
      const { data } = await axios.post<ExperimentAdSetWorkflowDto>(
        `/api/experiments/${experimentId}/adset-playbook/start`,
        { restart },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiment-adset-workflow", experimentId] });
    },
  });
}
