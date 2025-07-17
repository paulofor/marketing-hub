import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Experiment } from "./useExperiments";

export function useExperiment(id: number) {
  return useQuery({
    queryKey: ["experiment", id],
    queryFn: async () => {
      const { data } = await axios.get<Experiment>(`/api/experiments/${id}`);
      return data;
    },
  });
}
