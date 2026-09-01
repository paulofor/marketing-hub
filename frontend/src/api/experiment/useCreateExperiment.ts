import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  Experiment,
  ExperimentCampaignObjective,
  ExperimentPlatform,
  ExperimentStage,
  ExperimentType,
  ProductAiSubtype,
} from "./useExperiments";

export interface CreateExperiment {
  nicheId: number;
  productId: number;
  desireTerritoryCode: string;
  hypothesisId?: string;
  name?: string;
  hypothesis: string;
  singlePain?: string;
  freeReward?: string;
  funnelPromise?: string;
  primaryCta?: string;
  experimentType?: ExperimentType;
  productAiSubtype?: ProductAiSubtype | null;
  campaignObjective?: ExperimentCampaignObjective;
  platform: ExperimentPlatform;
  stage: ExperimentStage;
  primaryVariable?: string;
  primaryMetric?: string;
  commercialObjective?: string;
  kpiTarget?: number;
  metricPresetId?: string;
  sampleSize?: number;
  baselineCvr?: number;
  targetCvr?: number;
  mde?: number;
  dailyBudget?: number;
  mediaSpendLimit?: number;
  unitPrice: number;
  cost?: number;
  expense?: number;
  startDate?: string;
  endDate?: string;
  creativesToGenerate?: number;
  instantFormsToGenerate?: number;
  emailsToGenerate?: number;
  imagesPerPackage?: number;
  openImagesPerPackage?: number;
  compressedImagesPerPackage?: number;
  journeyTemplateId?: number;
  facebookPageId?: number;
  facebookInstantFormId?: number;
  instagramAccountId?: number;
  imageModelId?: number;
  imageModelQualityId?: number;
  schemaFirstLeadPortalEnabled?: boolean;
  creativeTextPrompt?: string;
  creativeImagePrompt?: string;
  promiseGenerationRequestIds?: number[];
}

export function useCreateExperiment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateExperiment) => {
      const { nicheId, ...payload } = data;
      const { data: experiment } = await axios.post<Experiment>(
        `/api/experiments`,
        {
          ...payload,
          marketNicheId: nicheId,
        },
      );
      return experiment;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
