import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Creative } from "./useCreatives";

export interface VideoCreativeInput {
  headline: string;
  primaryText: string;
  description?: string;
  replacesVideoAssetId?: number;
}

export function useCreateVideoCreative(
  experimentId: string | number,
  videoId: number,
) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (input: VideoCreativeInput) => {
      const { data } = await axios.post<Creative>(
        `/api/experiments/${experimentId}/video-assets/${videoId}/creative`,
        input,
      );
      return data;
    },
    onSuccess: async () => {
      await client.invalidateQueries({
        predicate: ({ queryKey }) =>
          (queryKey[0] === "products" && queryKey[2] === "ads") ||
          ([
            "creatives",
            "experiment",
            "experiment-readiness",
            "experiment-video-assets",
            "experiment-video-performance-dashboard",
            "experiment-history-events",
          ].includes(String(queryKey[0])) &&
            queryKey.some((key) => String(key) === String(experimentId))),
      });
    },
  });
}
