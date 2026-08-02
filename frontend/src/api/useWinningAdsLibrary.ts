import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface WinningAd {
  id: number;
  productSlug: string;
  productName: string;
  niche: string;
  funnelStage: string;
  channel: string;
  format: string;
  winningStatus: string;
  score: number;
  hook: string;
  primaryText: string;
  creativeBrief: string;
  offerAngle: string;
  proofSignal: string;
  metricSnapshot: string;
  learning: string;
  nextAction: string;
  sourceReference: string;
  updatedAt: string;
}

export interface WinningAdListResponse {
  total: number;
  items: WinningAd[];
}

export function useWinningAdsLibrary(productSlug?: string) {
  return useQuery({
    queryKey: ["winning-ads-library", productSlug || "all"],
    queryFn: async () => {
      const { data } = await axios.get<WinningAdListResponse>(
        "/api/winning-ads-library",
        { params: { productSlug: productSlug || undefined } },
      );
      return data;
    },
  });
}
