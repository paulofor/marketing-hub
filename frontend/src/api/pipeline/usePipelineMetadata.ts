import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { PipelineMetadata } from "./types";

export function usePipelineMetadata() {
  return useQuery({
    queryKey: ["pipelines", "metadata"],
    queryFn: async () => {
      const { data } = await axios.get<PipelineMetadata>(
        "/api/pipelines/metadata",
      );
      return data;
    },
  });
}
