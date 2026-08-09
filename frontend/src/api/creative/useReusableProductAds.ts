import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface ReusableProductAd {
  creativeId: number;
  experimentId: number;
  experimentName: string;
  headline?: string | null;
  imageUrl?: string | null;
  status: string;
  agentReviewStatus?: string | null;
}

interface ProductAdLibraryResponse {
  productId: number;
  ads: ReusableProductAd[];
}

export function useReusableProductAds(experimentId: string) {
  return useQuery({
    queryKey: ["reusable-product-ads", experimentId],
    queryFn: async () => {
      const current = await axios.get<ProductAdLibraryResponse>(
        `/api/products/experiments/${experimentId}/ads-in-use`,
      );
      const library = await axios.get<ProductAdLibraryResponse>(
        `/api/products/${current.data.productId}/ads`,
      );
      return library.data.ads.filter(
        (ad) =>
          ad.experimentId !== Number(experimentId) &&
          ad.status === "READY" &&
          ad.agentReviewStatus === "APPROVED",
      );
    },
  });
}

export function useReuseProductAd(experimentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (sourceCreativeId: number) => {
      await axios.post(
        `/api/experiments/${experimentId}/creatives/reuse/${sourceCreativeId}`,
      );
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["creatives", experimentId] });
      await queryClient.invalidateQueries({
        queryKey: ["reusable-product-ads", experimentId],
      });
    },
  });
}
