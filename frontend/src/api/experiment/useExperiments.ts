import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Experiment {
  id: number;
  hypothesis: string;
  kpiGoal: number;
  startDate: string;
  endDate: string;
  status: string;
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
