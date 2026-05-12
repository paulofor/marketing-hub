import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface HotmartCollectedProduct {
  jobId: string;
  referenceId: string;
  title: string;
  productUrl: string;
  producerName?: string | null;
  imageUrl?: string | null;
  successScore?: number | null;
  price?: string | null;
  currency?: string | null;
  collectedAt?: string | null;
}

interface HotmartCollectedProductListResponse {
  workspaceId: string;
  items: HotmartCollectedProduct[];
}

export function useHotmartCollectedProducts(workspaceId: string, limit = 24) {
  return useQuery({
    queryKey: ["settings", "hotmart", "products", workspaceId, limit],
    enabled: workspaceId.trim().length > 0,
    queryFn: async () => {
      const { data } = await axios.get<HotmartCollectedProductListResponse>("/api/settings/hotmart/products", {
        params: { workspaceId, limit },
      });
      return data.items;
    },
  });
}
