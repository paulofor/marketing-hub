import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type {
  ExperimentCampaignObjective,
  ExperimentStage,
  ExperimentType,
  ProductAiSubtype,
} from "../experiment/useExperiments";

export interface ProductAiExperimentDraft {
  experimentType: ExperimentType;
  productAiSubtype: ProductAiSubtype;
  stage: ExperimentStage;
  campaignObjective: ExperimentCampaignObjective;
  primaryVariable: string;
  primaryMetric: string;
  unitPrice?: number | null;
}

export interface ProductAiExperimentPreparation {
  hypothesisId: string;
  hypothesisTitle: string;
  productAiSubtype?: ProductAiSubtype | null;
  ready: boolean;
  blockers: string[];
  draft?: ProductAiExperimentDraft | null;
}

export function useProductAiExperimentPreparation(hypothesisId?: string) {
  return useQuery({
    queryKey: ["product-ai-experiment-preparation", hypothesisId],
    enabled: Boolean(hypothesisId),
    queryFn: async () => {
      const { data } = await axios.get<ProductAiExperimentPreparation>(
        `/api/product-ai/experiment-preparations/${hypothesisId}`,
      );
      return data;
    },
  });
}
