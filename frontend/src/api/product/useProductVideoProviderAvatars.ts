import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ProductVideoProviderAvatar = {
  id: number;
  productId: number;
  sourceAssetId: number;
  provider: string;
  characterName: string;
  providerAvatarId?: string | null;
  providerAvatarGroupId?: string | null;
  providerStatus?: string | null;
  sourceImageUrl?: string | null;
  supportsReusableAvatar: boolean;
  notes?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export function useProductVideoProviderAvatars(productId?: string | number) {
  return useQuery({
    queryKey: ["product", productId, "video-provider-avatars"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<ProductVideoProviderAvatar[]>(
        `/api/products/${productId}/video-provider-avatars`,
      );
      return data;
    },
  });
}
