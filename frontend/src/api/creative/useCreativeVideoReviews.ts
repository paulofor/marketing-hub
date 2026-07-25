import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type CreativeVideoReviewStatus = "DRAFT" | "READY" | "REJECTED";
export type CreativeVideoReviewSourceType = "CREATIVE" | "EXPERIMENT_VIDEO_ASSET";

export interface CreativeVideoReview {
  id: number;
  sourceType: CreativeVideoReviewSourceType;
  experimentId: number;
  experimentName: string;
  experimentStatus: string;
  hypothesisId?: string | null;
  hypothesisTitle?: string | null;
  hypothesisStatus?: string | null;
  nicheId?: number | null;
  nicheName?: string | null;
  format: string;
  headline: string;
  primaryText: string;
  videoId?: string | null;
  videoUrl?: string | null;
  description?: string | null;
  cta?: string | null;
  destinationUrl?: string | null;
  status: CreativeVideoReviewStatus;
  rejectionReason?: string | null;
  reviewedAt?: string | null;
  videoCostUsd?: number | string | null;
  audioCostUsd?: number | string | null;
  totalProductionCostUsd?: number | string | null;
  visualSourceType?: string | null;
  visualSourceKey?: string | null;
  visualSourceDescription?: string | null;
  visualSimilarityOverrideReason?: string | null;
}

export function useCreativeVideoReviews(status?: CreativeVideoReviewStatus | "ALL") {
  return useQuery({
    queryKey: ["creative-video-reviews", status ?? "ALL"],
    queryFn: async () => {
      const { data } = await axios.get<CreativeVideoReview[]>(
        "/api/creatives/video-review",
        {
          params: status && status !== "ALL" ? { status } : undefined,
        },
      );
      return data;
    },
  });
}

export function useUpdateCreativeVideoReviewStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      sourceType,
      status,
      rejectionReason,
    }: {
      id: number;
      sourceType: CreativeVideoReviewSourceType;
      status: CreativeVideoReviewStatus;
      rejectionReason?: string;
    }) => {
      const { data } = await axios.patch(`/api/creatives/video-review/${sourceType}/${id}/status`, {
        status,
        rejectionReason,
      });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["creative-video-reviews"] });
      queryClient.invalidateQueries({ queryKey: ["creatives"] });
      queryClient.invalidateQueries({ queryKey: ["experiments"] });
    },
  });
}
