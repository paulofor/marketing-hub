import { useQueries } from "@tanstack/react-query";
import axios from "axios";
import type { Pipeline, PipelineDiagnostics } from "./types";

export function usePipelineDiagnostics(pipelines: Pipeline[]) {
  return useQueries({
    queries: pipelines.map((pipeline) => ({
      queryKey: ["pipelines", pipeline.id, "diagnostics"],
      queryFn: async () => {
        const { data } = await axios.get<PipelineDiagnostics>(
          `/api/pipelines/${pipeline.id}/diagnostics`,
        );
        return data;
      },
      enabled: pipelines.length > 0,
    })),
  });
}
