import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  ExperimentVideoAsset,
  ExperimentVideoReviewStatus,
} from "./useExperimentVideoAssets";

interface UpdateExperimentVideoAssetReviewPayload {
  experimentId: number;
  videoAssetId: number;
  reviewStatus?: ExperimentVideoReviewStatus;
  rejectionReason?: string;
  reviewedBy?: string;
  hlsPlaybackUrl?: string;
}

export function useUpdateExperimentVideoAssetReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      experimentId,
      videoAssetId,
      reviewStatus,
      rejectionReason,
      reviewedBy,
      hlsPlaybackUrl,
    }: UpdateExperimentVideoAssetReviewPayload) => {
      const { data } = await axios.patch<ExperimentVideoAsset>(
        `/api/experiments/${experimentId}/video-assets/${videoAssetId}`,
        {
          reviewStatus,
          rejectionReason,
          reviewedBy,
          hlsPlaybackUrl,
        },
      );
      return data;
    },
    onSuccess: (videoAsset) => {
      queryClient.invalidateQueries({
        queryKey: ["experiment-video-assets", videoAsset.experimentId],
      });
      queryClient.invalidateQueries({
        queryKey: ["experiment-video-assets", "all"],
      });
    },
  });
}
