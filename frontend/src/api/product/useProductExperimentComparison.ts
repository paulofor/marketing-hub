import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ProductExperimentComparisonFunnelStage {
  stageCode: string;
  stageLabel: string;
  total: number;
}

export interface ProductExperimentComparisonExperiment {
  experimentId: number;
  name: string;
  status?: string | null;
  campaignStatus?: string | null;
  campaignObjective?: string | null;
  experimentType?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  dailyBudget?: number | null;
  unitPrice?: number | null;
  impressions: number;
  reach: number;
  clicks: number;
  leads: number;
  spend: number;
  cpc: number;
  cpl: number;
  approvedCreatives: number;
  totalCreatives: number;
  funnelStages: ProductExperimentComparisonFunnelStage[];
  hypothesis?: string | null;
  promise?: string | null;
  learnedLessons?: string | null;
  recommendedAction: string;
  updatedAt?: string | null;
}

export interface ProductExperimentComparison {
  productId: number;
  productName?: string | null;
  productSlug?: string | null;
  commercialStatus?: string | null;
  mainRecommendation: string;
  experiments: ProductExperimentComparisonExperiment[];
}

export function useProductExperimentComparison(productId?: string | number) {
  return useQuery<ProductExperimentComparison>({
    queryKey: ["products", productId, "experiment-comparison"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<ProductExperimentComparison>(
        `/api/products/${productId}/experiment-comparison`,
      );
      return data;
    },
  });
}
