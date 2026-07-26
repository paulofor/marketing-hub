import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ProductOrganicVideoPlanItem {
  day: number;
  sequence: number;
  category: string;
  funnelStage: string;
  mentalShift: string;
  platformPriority: string;
  hook: string;
  scene: string;
  message: string;
  callToAction: string;
  primaryMetric: string;
  productionNotes: string[];
}

export interface ProductOrganicVideoDecisionRule {
  signal: string;
  condition: string;
  decision: string;
  commercialReason: string;
}

export interface ProductOrganicVideoPlan {
  productId: number;
  productName?: string;
  productSlug?: string;
  strategyName: string;
  objective: string;
  publishingWindow: string;
  channelPriority: string;
  mixRationale: string;
  videos: ProductOrganicVideoPlanItem[];
  decisionRules: ProductOrganicVideoDecisionRule[];
  operatingPrinciples: string[];
}

export function useProductOrganicVideoPlan(productId?: string | number) {
  return useQuery<ProductOrganicVideoPlan>({
    queryKey: ["products", productId, "organic-video-plan"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<ProductOrganicVideoPlan>(
        `/api/products/${productId}/organic-video-plan`,
      );
      return data;
    },
  });
}
