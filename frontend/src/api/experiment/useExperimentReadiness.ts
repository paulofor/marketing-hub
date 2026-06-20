import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { TargetingElementType } from "../targeting/types";
import type { ExperimentCaptureDestinationType } from "./useExperiments";

export type ExperimentReadinessIssueType =
  | "CREATIVE"
  | "LEAD_PORTAL_FLOW"
  | "INSTANT_FORM"
  | "TARGETING";

export interface ExperimentReadinessIssue {
  type: ExperimentReadinessIssueType;
  title: string;
  description: string;
  recommendation?: string | null;
  missingTargetingTypes: TargetingElementType[];
}

export interface ExperimentReadinessSummary {
  hasCreatives: boolean;
  creativeCount: number;
  captureDestinationType: ExperimentCaptureDestinationType;
  hasLeadPortalFlow: boolean;
  leadPortalFlowCount: number;
  hasInstantForm: boolean;
  instantFormCount: number;
  hasCompleteTargeting: boolean;
  missingTargetingTypes: TargetingElementType[];
  issues: ExperimentReadinessIssue[];
}

export function useExperimentReadiness(experimentId?: string) {
  return useQuery({
    queryKey: ["experiment-readiness", experimentId],
    enabled: Boolean(experimentId),
    queryFn: async () => {
      if (!experimentId) throw new Error("experiment id is required");
      const { data } = await axios.get<ExperimentReadinessSummary>(
        `/api/experiments/${experimentId}/readiness`,
      );
      return data;
    },
  });
}
