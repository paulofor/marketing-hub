import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoJob } from "./types";

export function useSalesVideoJobs(profileId?: string | number) {
  return useQuery({
    queryKey: ["sales-video-jobs", profileId],
    enabled: Boolean(profileId),
    queryFn: async () => {
      const { data } = await axios.get<SalesVideoJob[]>(
        `/api/sales-videos/profiles/${profileId}/jobs`,
      );
      return data;
    },
  });
}
