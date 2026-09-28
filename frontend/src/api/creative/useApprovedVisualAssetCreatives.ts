import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Creative } from "./useCreatives";

export interface ApprovedVisualAssetOption {
  id: number;
  commercialPlanId: number;
  assetUrl: string;
  label: string;
  contentSha256: string;
  origin: string;
  rightsStatement: string;
}

export interface ApprovedVisualAssetCreativeInput {
  headline: string;
  primaryText: string;
  description?: string;
}

export function useApprovedVisualAssets(experimentId: string) {
  return useQuery({
    queryKey: ["approved-visual-assets", experimentId],
    queryFn: async () => {
      const { data } = await axios.get<ApprovedVisualAssetOption[]>(
        `/api/experiments/${experimentId}/commercial-plan-visual-assets/eligible`,
      );
      return data;
    },
  });
}

export function useCreateApprovedVisualAssetCreative(
  experimentId: string | number,
  assetId: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: ApprovedVisualAssetCreativeInput) => {
      const { data } = await axios.post<Creative>(
        `/api/experiments/${experimentId}/commercial-plan-visual-assets/${assetId}/creative`,
        input,
      );
      return data;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        predicate: ({ queryKey }) =>
          ([
            "creatives",
            "experiment",
            "experiment-readiness",
            "experiment-history-events",
            "approved-visual-assets",
          ].includes(String(queryKey[0])) &&
            queryKey.some((key) => String(key) === String(experimentId))) ||
          (queryKey[0] === "products" && queryKey[2] === "ads"),
      });
    },
  });
}
