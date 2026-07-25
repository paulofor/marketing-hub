import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { resolveAssetUrl } from "../../utils/resolveAssetUrl";

export type ProductVideoImage = {
  id: number;
  productId: number;
  assetId: number;
  assetType: string;
  provider: string;
  assetStatus: string;
  url?: string | null;
  publicUrl?: string;
  model?: string | null;
  purpose: string;
  prompt: string;
  reviewStatus: "PENDING" | "APPROVED" | "REJECTED";
  reviewNotes?: string | null;
  createdAt: string;
};

function normalizeProductVideoImage(
  image: ProductVideoImage,
): ProductVideoImage {
  return {
    ...image,
    publicUrl: resolveAssetUrl(image.url),
  };
}

export function useProductVideoImages(productId?: string | number) {
  return useQuery({
    queryKey: ["product", productId, "video-images"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<ProductVideoImage[]>(
        `/api/products/${productId}/video-images`,
      );
      return data.map(normalizeProductVideoImage);
    },
  });
}

export function useGenerateProductVideoImages(productId?: string | number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: { prompt: string }) => {
      const { data } = await axios.post<ProductVideoImage[]>(
        `/api/products/${productId}/video-images/generations`,
        payload,
        { timeout: 240000 },
      );
      return data.map(normalizeProductVideoImage);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["product", productId, "video-images"],
      });
    },
  });
}
