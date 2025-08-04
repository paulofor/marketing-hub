import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Experiment {
  id: string;
  nicheId: number;
  hypothesisId: string;
  name: string;
  hypothesis: string;
  kpiTarget: number;
  sampleSize?: number | null;
  mdePercent?: number | null;
  startDate: string | null;
  endDate: string | null;
  metricPresetId?: string | null;
  status: string;
  platform: string;
  createdAt: string;
  updatedAt: string;
}

export function useExperiments() {
  return useQuery({
    queryKey: ["experiments"],
    queryFn: async () => {
      const { data } = await axios.get<Experiment[]>("/api/experiments");
      return data;
    },
  });
}
