import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface GeraLandingStageExecutionItem {
  idJob: string;
  status: string;
  executionRequestedAt: string;
}

export function useGeraLandingStageExecutions(
  experimentId: string,
  stageCode = "landing-page-wireframe",
) {
  return useQuery({
    queryKey: ["geralanding-stage-executions", experimentId, stageCode],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<GeraLandingStageExecutionItem[]>(
        `/api/experiments/${experimentId}/geralanding/stage-executions`,
        { params: { stageCode } },
      );
      return data;
    },
  });
}
