import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { Pipeline } from "./types";

export function usePipelines() {
  return useQuery({
    queryKey: ["pipelines"],
    queryFn: async () => {
      const { data } = await axios.get<Pipeline[]>("/api/pipelines");
      return data;
    },
  });
}
