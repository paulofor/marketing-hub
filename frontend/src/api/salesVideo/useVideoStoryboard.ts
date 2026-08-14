import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type VideoStoryboardScene = {
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
