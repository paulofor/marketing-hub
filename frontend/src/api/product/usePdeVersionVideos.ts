import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type {
  ExperimentVideoReviewStatus,
  ExperimentVideoStatus,
} from "../experiment/useExperimentVideoAssets";
import type { PostDeployPdeProductionSlot } from "../experiment/usePostDeployMonitor";

export interface PdeVersionVideoAsset {
  id?: number | null;
  experimentId?: number | null;
  assignmentSource:
    | "VERSION_TOKEN"
    | "SOURCE_EXPERIMENT"
    | "PUBLISHED_CONTRACT";
  objective: string;
  primaryMetric: string;
  provider: string;
  model: string;
  status: ExperimentVideoStatus;
  reviewStatus: ExperimentVideoReviewStatus;
  assetUrl?: string | null;
  hlsPlaybackUrl?: string | null;
  thumbnailUrl?: string | null;
  durationSeconds?: number | null;
  salesVideoProfileId?: number | null;
  salesVideoJobId?: number | null;
  assetId?: number | null;
  landingVideoSlotId?: number | null;
}

export interface PdeVersionVideoPanel {
  slot: PostDeployPdeProductionSlot;
  videos: PdeVersionVideoAsset[];
  alerts: string[];
}

export function useProductPdeVersionVideos(productId?: string | number) {
  return useQuery<PdeVersionVideoPanel[]>({
    queryKey: ["products", productId, "pde-videos"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<PdeVersionVideoPanel[]>(
        `/api/products/${productId}/pde-videos`,
      );
      return data;
    },
  });
}
