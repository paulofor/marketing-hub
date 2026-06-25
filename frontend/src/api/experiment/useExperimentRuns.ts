import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type ExperimentRunMode = "TEST" | "PRODUCTION";
export type ExperimentRunStatus =
  | "DRAFT"
  | "PREFLIGHT_PENDING"
  | "PREFLIGHT_RUNNING"
  | "PREFLIGHT_FAILED"
  | "READY_TO_PUBLISH"
  | "PUBLICATION_PENDING"
  | "PUBLISHING"
  | "PUBLISHED_AWAITING_EXPOSURE"
  | "RUNNING"
  | "PAUSE_REQUESTED"
  | "PAUSED"
  | "STOP_REQUESTED"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED";
export type ExperimentEvidenceValidity =
  | "NOT_EVALUATED"
  | "TECHNICALLY_INVALID"
  | "MEASUREMENT_INVALID"
  | "STRATEGICALLY_INVALID"
  | "INSUFFICIENT_DATA"
  | "COMMERCIALLY_VALID";
export type ExperimentRunDataQualityStatus =
  | "UNKNOWN"
  | "VALID"
  | "WARNING"
  | "BLOCKED"
  | "STALE";
export type ExperimentRunStopPolicy =
  | "FIRST_VALID_LEAD_STANDBY"
  | "FIXED_WINDOW"
  | "MIN_SAMPLE_AND_WINDOW"
  | "STOP_LOSS"
  | "MANUAL_ONLY";
export type ExperimentRunGateStatus =
  | "PASS"
  | "WARNING"
  | "FAIL"
  | "NOT_APPLICABLE"
  | "PENDING";
export type ExperimentRunGateSeverity = "INFO" | "WARNING" | "BLOCKER";
export type ExperimentRunGateGroup =
  | "UPSTREAM_QUALITY"
  | "EXPERIMENT_DESIGN"
  | "ASSET_QUALITY"
  | "FUNCTIONAL_E2E"
  | "META_PUBLICATION"
  | "MEASUREMENT";

export type ExperimentRun = {
  id: number;
  experimentId: number;
  runNumber: number;
  mode: ExperimentRunMode;
  status: ExperimentRunStatus;
  evidenceValidity: ExperimentEvidenceValidity;
  dataQualityStatus: ExperimentRunDataQualityStatus;
  stopPolicy: ExperimentRunStopPolicy;
  requestedAt: string;
  preflightStartedAt?: string | null;
  preflightCompletedAt?: string | null;
  firstVerifiedImpressionAt?: string | null;
  commercialWindowStartedAt?: string | null;
  createdBy?: string | null;
};

export type ExperimentRunGateResult = {
  gateCode: string;
  gateGroup: ExperimentRunGateGroup;
  status: ExperimentRunGateStatus;
  severity: ExperimentRunGateSeverity;
  summary: string;
  evidenceReference?: string | null;
  remediationCode?: string | null;
  evaluatedAt: string;
  evaluatorType: string;
  evaluatorVersion?: string | null;
};

export type ExperimentRunPreflight = {
  runId: number;
  runStatus: ExperimentRunStatus;
  hasBlockers: boolean;
  gates: ExperimentRunGateResult[];
};

const experimentRunsQueryKey = (experimentId?: string | number) => [
  "experiment-runs",
  experimentId,
];

const experimentRunPreflightQueryKey = (runId?: number) => [
  "experiment-run-preflight",
  runId,
];

export function useExperimentRuns(experimentId?: string | number) {
  return useQuery({
    queryKey: experimentRunsQueryKey(experimentId),
    enabled: Boolean(experimentId),
    queryFn: async () => {
      const { data } = await axios.get<ExperimentRun[]>(
        `/api/experiments/${experimentId}/runs`,
      );
      return data;
    },
  });
}

export function useExperimentRunPreflight(runId?: number) {
  return useQuery({
    queryKey: experimentRunPreflightQueryKey(runId),
    enabled: Boolean(runId),
    queryFn: async () => {
      const { data } = await axios.get<ExperimentRunPreflight>(
        `/api/experiment-runs/${runId}/preflight`,
      );
      return data;
    },
  });
}

export function useCreateExperimentRun(experimentId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (mode: ExperimentRunMode) => {
      const { data } = await axios.post<ExperimentRun>(
        `/api/experiments/${experimentId}/runs`,
        { mode, stopPolicy: "MANUAL_ONLY" },
      );
      return data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: experimentRunsQueryKey(experimentId),
      });
    },
  });
}

export function useRunExperimentPreflight(experimentId?: string | number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (runId: number) => {
      const { data } = await axios.post<ExperimentRunPreflight>(
        `/api/experiment-runs/${runId}/preflight`,
      );
      return data;
    },
    onSuccess: async (preflight) => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: experimentRunsQueryKey(experimentId),
        }),
        queryClient.invalidateQueries({
          queryKey: experimentRunPreflightQueryKey(preflight.runId),
        }),
      ]);
    },
  });
}
