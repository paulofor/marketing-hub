import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ExperimentDiagnosticsSeverity = "INFO" | "WARNING" | "ERROR";

export interface ExperimentPublishingArtifact {
  type: "CAMPAIGN" | "AD_SET" | "AD";
  id: string;
  name: string;
  status: string;
  externalId: string | null;
}

export interface ExperimentDiagnostics {
  severity: ExperimentDiagnosticsSeverity;
  headline: string;
  description: string;
  resolution: string | null;
  artifacts: ExperimentPublishingArtifact[];
}

export function useExperimentDiagnostics(id?: string) {
  return useQuery({
    queryKey: ["experiment-diagnostics", id],
    enabled: Boolean(id),
    queryFn: async () => {
      if (!id) throw new Error("experiment id is required");
      const { data } = await axios.get<ExperimentDiagnostics>(
        `/api/experiments/${id}/diagnostics`,
      );
      return data;
    },
  });
}
