import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Product } from "./useProducts";

export type ProductVideoSeedImageReviewStatus =
  "PENDING" | "APPROVED" | "REJECTED";

export interface UpdateProductVideoSeedImagePayload {
  productId: number;
  assetId: number;
  characterName: string;
  reviewStatus: ProductVideoSeedImageReviewStatus;
  reviewNotes?: string;
  reviewedBy?: string;
}

export function useUpdateProductVideoSeedImage() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      productId,
      assetId,
      characterName,
      reviewStatus,
      reviewNotes,
      reviewedBy,
    }: UpdateProductVideoSeedImagePayload) => {
      const { data } = await axios.patch<Product>(
        `/api/products/${productId}/video-seed-image`,
        {
          assetId,
          characterName,
          reviewStatus,
          reviewNotes,
          reviewedBy,
        },
      );
      return data;
    },
    onSuccess: (product) => {
      queryClient.invalidateQueries({ queryKey: ["product", product.id] });
      queryClient.invalidateQueries({
        queryKey: ["product", product.id, "video-images"],
      });
      queryClient.invalidateQueries({
        queryKey: ["product", String(product.id), "video-images"],
      });
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
  });
}
