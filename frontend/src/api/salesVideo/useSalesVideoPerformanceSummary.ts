import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoPerformanceSummary } from "./types";

export function useSalesVideoPerformanceSummary(profileId?: string | number) {
  return useQuery({
    queryKey: ["sales-video-performance-summary", profileId],
    enabled: Boolean(profileId),
    queryFn: async () => {
      const { data } = await axios.get<SalesVideoPerformanceSummary>(
        `/api/sales-videos/profiles/${profileId}/performance-summary`,
      );
      return data;
    },
  });
}
