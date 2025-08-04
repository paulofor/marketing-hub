import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface MetricPreset {
  id: string;
  name: string;
  sampleSize: number;
  stopLossFactor: number;
  defaultMdePp: number;
}

export function useMetricPresets() {
  return useQuery({
    queryKey: ["metric-presets"],
    queryFn: async () => {
      const { data } = await axios.get<MetricPreset[]>("/api/metric-presets");
      return data;
    },
  });
}
