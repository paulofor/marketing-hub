import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Creative {
  id: number;
  sourceCreativeId?: number | null;
  versionNumber?: number;
  finalCandidate?: boolean;
  experimentId: number;
  headline: string;
  primaryText: string;
  imageUrl: string;
  videoId?: string | null;
  videoUrl?: string | null;
  status: string;
  format?: string;
  description?: string;
  cta?: string;
  destinationUrl?: string;
  leadGenFormId?: string;
  instagramUserId?: string;
  imagePrompt?: string;
  imageIntermediatePrompt?: string;
  agentReviewStatus?:
    | "PENDING"
    | "PROCESSING"
    | "APPROVED"
    | "ADJUST"
    | "REJECTED"
    | "FAILED"
    | null;
  agentReviewJson?: string | null;
  agentReviewModel?: string | null;
  agentReviewedAt?: string | null;
  agentImprovementStatus?:
    "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "LIMIT_REACHED" | null;
  agentImprovementAttempts?: number | null;
  agentImprovementError?: string | null;
}

interface ExperimentProductAd {
  creativeId: number;
  sourceCreativeId?: number | null;
  versionNumber?: number | null;
  finalCandidate?: boolean;
  experimentId: number;
  headline?: string | null;
  primaryText?: string | null;
  imageUrl?: string | null;
  videoId?: string | null;
  videoUrl?: string | null;
  status?: string | null;
  format?: string | null;
  description?: string | null;
  cta?: string | null;
  destinationUrl?: string | null;
  agentReviewStatus?: Creative["agentReviewStatus"];
  agentReviewJson?: string | null;
  agentReviewModel?: string | null;
  agentReviewedAt?: string | null;
  agentImprovementStatus?: Creative["agentImprovementStatus"];
  agentImprovementAttempts?: number | null;
  agentImprovementError?: string | null;
}

interface ExperimentProductAdsResponse {
  ads: ExperimentProductAd[];
}

function mapProductAdToCreative(ad: ExperimentProductAd): Creative {
  return {
    id: ad.creativeId,
    sourceCreativeId: ad.sourceCreativeId ?? null,
    versionNumber: ad.versionNumber ?? 1,
    finalCandidate: ad.finalCandidate ?? false,
    experimentId: ad.experimentId,
    headline: ad.headline ?? "",
    primaryText: ad.primaryText ?? "",
    imageUrl: ad.imageUrl ?? "",
    videoId: ad.videoId ?? null,
    videoUrl: ad.videoUrl ?? null,
    status: ad.status ?? "DRAFT",
    format: ad.format ?? undefined,
    description: ad.description ?? undefined,
    cta: ad.cta ?? undefined,
    destinationUrl: ad.destinationUrl ?? undefined,
    agentReviewStatus: ad.agentReviewStatus ?? null,
    agentReviewJson: ad.agentReviewJson ?? null,
    agentReviewModel: ad.agentReviewModel ?? null,
    agentReviewedAt: ad.agentReviewedAt ?? null,
    agentImprovementStatus: ad.agentImprovementStatus ?? null,
    agentImprovementAttempts: ad.agentImprovementAttempts ?? 0,
    agentImprovementError: ad.agentImprovementError ?? null,
  };
}

export function useCreatives(expId: string) {
  return useQuery({
    queryKey: ["creatives", expId],
    queryFn: async () => {
      const { data } = await axios.get<
        ExperimentProductAdsResponse | Creative[]
      >(`/api/products/experiments/${expId}/ads-in-use`);
      if (Array.isArray(data)) {
        return data;
      }
      return data.ads.map(mapProductAdToCreative);
    },
  });
}
