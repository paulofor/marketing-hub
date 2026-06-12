import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface OprmRoutineValueBlockPayload {
  id?: string | null;
  title?: string | null;
  blockType?: string | null;
  summary?: string | null;
  items?: string[] | null;
  tasks?: string[] | null;
  signals?: Record<string, unknown>[] | null;
}

export interface OprmRoutineCardPayload extends Record<string, unknown> {
  routineSummary?: string | null;
  topTasks?: Record<string, unknown>[] | string[] | null;
  topConstraints?: Record<string, unknown>[] | string[] | null;
  workaroundPatterns?: Record<string, unknown>[] | string[] | null;
  routineValueBlocks?: OprmRoutineValueBlockPayload[] | null;
  beforeServiceTasks?: string[] | null;
  duringServiceTasks?: string[] | null;
  afterServiceTasks?: string[] | null;
  betweenClientsAdministrationTasks?: string[] | null;
  acquisitionRetentionTasks?: string[] | null;
  observedPainsAndRisks?: string[] | null;
  productOpportunities?: string[] | null;
}

export interface OprmRoutineWorkspaceData {
  occupationSeedRef: string;
  lastCorrelationId: string | null;
  routineCardPayload: OprmRoutineCardPayload | null;
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
