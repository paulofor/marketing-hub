import { useQuery } from "@tanstack/react-query";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type VideoStoryboardScene = {
  consumptionId?: number | null;
  sceneNumber: number;
  commercialRole: string;
  plan?: string | null;
  jobId?: number | null;
  jobStatus?: string | null;
  providerTaskId?: string | null;
  requestedDurationSeconds?: number | null;
  expectedCredits?: number | null;
  consumedCredits?: number | null;
  producedFileUrl?: string | null;
  utilizationPercent?: number | null;
  utilizationEvidence: string;
  commercialEvaluationStatus?: string | null;
  commercialEvaluationNotes?: string | null;
  commercialEvaluatedBy?: string | null;
  commercialEvaluatedAt?: string | null;
};

export type VideoStoryboard = {
  projectId: number;
  plannedSceneCount: number;
  expectedCredits: number;
  consumedCredits: number;
  utilizationPercent?: number | null;
  plannerStatus?: string | null;
  plannerModel?: string | null;
  budgetGate?: string | null;
  expectedCostUsd?: number | null;
  scenes: VideoStoryboardScene[];
};

/** Consulta a verdade consolidada do storyboard persistida pelo backend. */
export function useVideoStoryboard(projectId?: number) {
  return useQuery({
    queryKey: ["video-storyboard", projectId],
    queryFn: async () => {
      const { data } = await axios.get<VideoStoryboard>(
        `/api/sales-videos/projects/${projectId}/storyboard`,
      );
      return data;
    },
    enabled: Boolean(projectId),
  });
}

/** Persiste a avaliação comercial de uma cena sem solicitar nova geração. */
export function useEvaluateStoryboardScene(projectId?: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: {
      consumptionId: number;
      status: string;
      utilizationPercent: number;
      notes: string;
      evaluatedBy: string;
    }) => {
      const { consumptionId, ...payload } = input;
      const { data } = await axios.patch<VideoStoryboard>(
        `/api/sales-videos/projects/${projectId}/storyboard/scenes/${consumptionId}/evaluation`,
        payload,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.setQueryData(["video-storyboard", projectId], data);
    },
  });
}
