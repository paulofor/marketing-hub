import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Creative {
  id: number;
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
}

interface ExperimentProductAd {
  creativeId: number;
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
}

interface ExperimentProductAdsResponse {
  ads: ExperimentProductAd[];
}

function mapProductAdToCreative(ad: ExperimentProductAd): Creative {
  return {
    id: ad.creativeId,
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
