import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { SalesVideoJobEvent } from "./types";

export function useSalesVideoJobEvents(jobId?: number) {
  return useQuery({
    queryKey: ["sales-video-job-events", jobId],
    enabled: Boolean(jobId),
    queryFn: async () => {
      const { data } = await axios.get<SalesVideoJobEvent[]>(`/api/sales-videos/jobs/${jobId}/events`);
      return data;
    },
  });
}
