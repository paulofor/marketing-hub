import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type CreativeVideoReviewStatus = "DRAFT" | "READY";

export interface CreativeVideoReview {
  id: number;
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
      status,
    }: {
      id: number;
      status: CreativeVideoReviewStatus;
    }) => {
      const { data } = await axios.patch(`/api/creatives/${id}/status`, {
        status,
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
