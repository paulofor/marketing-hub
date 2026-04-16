import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface OprmRoutineWorkspaceData {
  occupationSeedRef: string;
  lastCorrelationId: string | null;
  routineCardPayload: Record<string, unknown> | null;
  frameworkInputPayload: Record<string, unknown> | null;
  painSignals: Record<string, unknown>[];
  desiredOutcomeSignals: Record<string, unknown>[];
  mechanismOpportunitySignals: Record<string, unknown>[];
}

export function useOprmRoutineWorkspaceData(occupationSeedRef?: string) {
  return useQuery({
    queryKey: ["oprm", "routine", occupationSeedRef],
    enabled: Boolean(occupationSeedRef),
    queryFn: async () => {
      const { data } = await axios.get<OprmRoutineWorkspaceData>(
        `/api/oprm/workspace/routine/${encodeURIComponent(occupationSeedRef ?? "")}`,
      );
      return data;
    },
  });
}
