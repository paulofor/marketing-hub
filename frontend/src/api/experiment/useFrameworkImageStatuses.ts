import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FrameworkImageItemStatus {
  planningItemKey: string;
  sectionName?: string;
  prompt?: string;
  jobId?: string;
  status: string;
  stage?: string;
  model?: string;
  assetId?: number;
  sourceUrl?: string;
  webUrl?: string;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

function hasInFlightItem(items: FrameworkImageItemStatus[]) {
  return items.some((item) => {
    const normalized = item.status?.toUpperCase();
    return (
      normalized === "PLANNED" ||
      normalized === "PENDING" ||
      normalized === "PROCESSING"
    );
  });
}

export function useFrameworkImageStatuses(experimentId?: string) {
  return useQuery({
    queryKey: ["framework-image-statuses", experimentId],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      if (!experimentId) return [];
      const { data } = await axios.get<FrameworkImageItemStatus[]>(
        `/api/experiments/${experimentId}/framework-images`,
      );
      return data ?? [];
    },
    refetchInterval: (query) => {
      const items = (query.state.data as FrameworkImageItemStatus[] | undefined) ?? [];
      return hasInFlightItem(items) ? 5000 : 15000;
    },
  });
}
