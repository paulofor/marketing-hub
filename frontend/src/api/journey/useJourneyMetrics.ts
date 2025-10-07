import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { JourneyMetrics } from "./types";

export function useJourneyMetrics() {
  return useQuery({
    queryKey: ["journeys", "metrics"],
    queryFn: async () => {
      const { data } = await axios.get<JourneyMetrics>("/api/journeys/metrics");
      return data;
    },
  });
}
