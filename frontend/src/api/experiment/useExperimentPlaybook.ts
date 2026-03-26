import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { ExperimentStage } from "./useExperiments";

export interface ExperimentPlaybookVariable {
  id: string;
  label: string;
  description: string;
  aiOutputs: string[];
  suggestedPrimaryMetric?: string | null;
}

export interface ExperimentPlaybookStage {
  stage: ExperimentStage;
  title: string;
  description: string;
  defaultPrimaryMetric: string;
  guardrailMetrics: string[];
  variables: ExperimentPlaybookVariable[];
}

export function useExperimentPlaybook() {
  return useQuery({
    queryKey: ["experiment-playbook"],
    queryFn: async () => {
      const { data } = await axios.get<ExperimentPlaybookStage[]>(
        "/api/experiment-playbook",
      );
      return data;
    },
    staleTime: 1000 * 60 * 60, // 1 hora
  });
}
