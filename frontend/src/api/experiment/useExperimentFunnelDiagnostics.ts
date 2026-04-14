import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ExperimentFunnelStage } from "./useExperimentFunnel";

export type FunnelDiagnosticStatus =
  | "NO_DATA"
  | "INSUFFICIENT_DATA"
  | "TECHNICAL_ISSUE_SUSPECTED"
  | "WEAK_SIGNAL"
  | "STATISTICALLY_FAILED"
  | "HEALTHY_OR_INCONCLUSIVE";

export interface ExperimentFunnelStageDiagnostic {
  stageKey: ExperimentFunnelStage;
  stageLabel: string;
  attempts: number;
  successes: number;
  observedRate?: number | null;
  minAcceptableRate?: number | null;
  upper95RateIfZero?: number | null;
  status: FunnelDiagnosticStatus;
  reasonCode: string;
  message: string;
  technicalIssueSuspected: boolean;
}

export interface ExperimentFunnelDiagnosticsResponse {
  diagnostics: ExperimentFunnelStageDiagnostic[];
  contextualAlert?: string | null;
}

export function useExperimentFunnelDiagnostics(experimentId?: string) {
  return useQuery<ExperimentFunnelDiagnosticsResponse>({
    queryKey: ["experiment", experimentId, "funnel", "diagnostics"],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentFunnelDiagnosticsResponse>(
        `/api/experiments/${experimentId}/funnel/diagnostics`,
      );
      return data;
    },
    enabled: Boolean(experimentId),
  });
}
