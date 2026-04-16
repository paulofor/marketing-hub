import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface OprmArtifactSummary {
  artifactId: string;
  artifactType: string;
  artifactVersion: string;
  artifactStatus: string;
  occupationSeedRef: string;
  correlationId: string;
  createdAt: string;
}

export interface OprmInsightsWorkspaceData {
  occupationSeedRef: string;
  lastCorrelationId: string | null;
  timeline: OprmArtifactSummary[];
  sources: Record<string, unknown>[];
  excerpts: Record<string, unknown>[];
  lineage: Record<string, unknown>;
  feedbackSnapshots: Record<string, unknown>[];
  feedbackComparison: Record<string, unknown>;
}

export function useOprmInsightsWorkspaceData(occupationSeedRef?: string) {
  return useQuery({
    queryKey: ["oprm", "insights", occupationSeedRef],
    enabled: Boolean(occupationSeedRef),
    queryFn: async () => {
      const { data } = await axios.get<OprmInsightsWorkspaceData>(
        `/api/oprm/workspace/insights/${encodeURIComponent(occupationSeedRef ?? "")}`,
      );
      return data;
    },
  });
}
