import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ProductAdLibraryItem {
  creativeId: number;
  experimentId: number;
  experimentName?: string | null;
  experimentStatus?: string | null;
  format?: string | null;
  status?: string | null;
  headline?: string | null;
  primaryText?: string | null;
  description?: string | null;
  cta?: string | null;
  destinationUrl?: string | null;
  imageUrl?: string | null;
  videoUrl?: string | null;
  videoId?: string | null;
  reuseRecommendation: string;
  reviewedAt?: string | null;
}

export interface ProductAdLibrary {
  productId: number;
  productName?: string | null;
  productSlug?: string | null;
  commercialStatus?: string | null;
  mainRecommendation: string;
  ads: ProductAdLibraryItem[];
}

export function useProductAdLibrary(productId?: string | number) {
  return useQuery<ProductAdLibrary>({
    queryKey: ["products", productId, "ads"],
    enabled: Boolean(productId),
    queryFn: async () => {
      const { data } = await axios.get<ProductAdLibrary>(
        `/api/products/${productId}/ads`,
      );
      return data;
    },
  });
}
