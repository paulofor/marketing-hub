import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ExperimentEmailDetail {
  experimentId: number;
  journeyId: number;
  stepId: number;
  stepName?: string | null;
  stepPosition?: number | null;
  stepPhase?: string | null;
  stepDescription?: string | null;
  stepMetadata?: Record<string, string> | null;
  subject?: string | null;
  templateId?: string | null;
  status?: string | null;
  notes?: string | null;
  preheader?: string | null;
  model?: string | null;
  prompt?: string | null;
  approved: boolean;
  journeyCreatedAt?: string | null;
  journeyUpdatedAt?: string | null;
}

export function useExperimentEmailDetail(experimentId?: string, stepId?: string) {
  return useQuery({
    queryKey: ["experiment-email", experimentId, stepId],
    enabled: Boolean(experimentId && stepId),
    queryFn: async () => {
      if (!experimentId || !stepId) return null;
      const { data } = await axios.get<ExperimentEmailDetail>(
        `/api/experiments/${experimentId}/emails/${stepId}`,
      );
      return data;
    },
  });
}
