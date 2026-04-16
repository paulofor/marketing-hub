import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { OprmArtifactSummary } from "./useOprmInsightsWorkspaceData";

export type OprmArtifactStatus =
  | "DRAFT"
  | "GENERATED"
  | "VALIDATED"
  | "REJECTED"
  | "PUBLISHED"
  | "SUPERSEDED";

export function useOprmFailedArtifacts() {
  return useQuery({
    queryKey: ["oprm", "operations", "failed-artifacts"],
    queryFn: async () => {
      const { data } = await axios.get<OprmArtifactSummary[]>("/api/oprm/artifacts", {
        params: { status: "REJECTED" satisfies OprmArtifactStatus },
      });
      return data;
    },
  });
}

export function useOprmArtifactsByCorrelationId(correlationId?: string) {
  return useQuery({
    queryKey: ["oprm", "operations", "correlation", correlationId],
    enabled: Boolean(correlationId),
    queryFn: async () => {
      const { data } = await axios.get<OprmArtifactSummary[]>("/api/oprm/artifacts", {
        params: { correlationId },
      });
      return data;
    },
  });
}
