import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ExperimentLearningStatus =
  | "PENDING"
  | "PROCESSING"
  | "READY"
  | "FAILED";

export interface ExperimentLearningRequest {
  id: number;
  experimentId: number;
  status: ExperimentLearningStatus;
  requestedAt: string;
  completedAt?: string | null;
  requestedBy?: string | null;
  failureReason?: string | null;
}

export function useExperimentLearningRequests(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-learning-requests", experimentId],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentLearningRequest[]>(
        `/api/experiments/${experimentId}/learning-requests`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
  });
}
