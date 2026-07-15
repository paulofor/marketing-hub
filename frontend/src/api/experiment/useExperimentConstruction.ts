import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ExperimentCreationSource } from "./useExperiments";

export interface ExperimentConstructionItem {
  label: string;
  value: string;
}

export interface ExperimentConstructionSection {
  title: string;
  description?: string | null;
  items: ExperimentConstructionItem[];
}

export interface ExperimentConstructionStep {
  code: string;
  title: string;
  description: string;
  tab: string;
  action: string;
  validated: boolean;
  validationLabel?: string | null;
}

export interface ExperimentConstruction {
  experimentId: number;
  experimentName: string;
  creationSource: ExperimentCreationSource;
  manualFlow: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
  flowSteps: ExperimentConstructionStep[];
  sections: ExperimentConstructionSection[];
}

export function useExperimentConstruction(
  experimentId?: string,
  enabled = true,
) {
  return useQuery({
    queryKey: ["experiment-construction", experimentId],
    enabled: Boolean(experimentId) && enabled,
    queryFn: async () => {
      const { data } = await axios.get<ExperimentConstruction>(
        `/api/experiments/${experimentId}/construction`,
      );
      return data;
    },
  });
}
