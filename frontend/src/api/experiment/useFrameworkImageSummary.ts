import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FrameworkImageSummary {
  totalItems: number;
  plannedCount: number;
  processingCount: number;
  waitingOpenAiBatchCount: number;
  completedCount: number;
  failedCount: number;
  updatedAt?: string;
}

export function useFrameworkImageSummary(experimentId?: string) {
  return useQuery({
    queryKey: ["framework-image-summary", experimentId],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      if (!experimentId) return null;
      const { data } = await axios.get<FrameworkImageSummary>(
        `/api/experiments/${experimentId}/framework-images/summary`,
      );
      return data;
    },
    refetchInterval: 15000,
  });
}
