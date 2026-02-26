import { type Query, useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingResolutionSummary, TargetingRequest } from "../targeting/types";

export interface ExperimentSimpleFlowStatus {
  request?: TargetingRequest | null;
  resolution?: TargetingResolutionSummary | null;
}

export function useExperimentSimpleFlowStatus(experimentId?: number) {
  return useQuery<ExperimentSimpleFlowStatus>({
    queryKey: ["experiment-simple-flow-status", experimentId],
    enabled: !!experimentId,
    refetchInterval: (query: Query<ExperimentSimpleFlowStatus, Error, ExperimentSimpleFlowStatus, readonly unknown[]>) => {
      const data = query.state.data;
      if (!data?.request) {
        return false;
      }
      const pending = (data.resolution?.pending ?? 0) + (data.resolution?.processing ?? 0);
      return pending > 0 ? 10000 : false;
    },
    queryFn: async () => {
      const { data } = await axios.get<ExperimentSimpleFlowStatus>(
        `/api/experiments/${experimentId}/targeting-selections/run-simple-flow/status`,
      );
      return data;
    },
  });
}
