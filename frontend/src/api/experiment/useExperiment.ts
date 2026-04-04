import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Experiment } from "./useExperiments";

export function useExperiment(id?: string) {
  return useQuery({
    queryKey: ["experiment", id],
    enabled: Boolean(id),
    queryFn: async () => {
      const { data } = await axios.get<Experiment>(`/api/experiments/${id}`);
      return data;
    },
  });
}
