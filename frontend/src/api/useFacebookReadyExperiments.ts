import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface FacebookReadyExperiment {
  id: number;
  name: string;
  hypothesis: string;
  kpiTargetCpl: number | null;
  startDate: string | null;
  endDate: string | null;
  nicheName: string | null;
  hypothesisTitle: string | null;
  missingConfiguration: string[];
}

export function useFacebookReadyExperiments() {
  return useQuery({
    queryKey: ["facebook-experiments-ready"],
    queryFn: async () => {
      const { data } = await axios.get<FacebookReadyExperiment[]>(
        "/api/facebook-campaigns/experiments-ready",
      );
      return data;
    },
  });
}
