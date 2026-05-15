import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface HotmartCollectedProduct {
  jobId: string;
  referenceId: string;
  title: string;
  productUrl: string;
  producerName?: string | null;
  imageUrl?: string | null;
  price?: string | null;
  currency?: string | null;
  salesPageUrl?: string | null;
  pageSalesLink?: string | null;
  temperature?: number | null;
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
      const { data } = await axios.get<HotmartCollectedProductListResponse>("/api/v1/mois/hotmart/products", {
        params: { workspaceId, limit },
      });
      return data.items;
    },
  });
}


export interface HotmartCollectionJob {
  jobId: string;
  workspaceId: string;
  status: string;
  createdAt?: string | null;
  message?: string | null;
}

interface HotmartCollectionJobListResponse {
  items: HotmartCollectionJob[];
}

export function useHotmartCollectionJobs(workspaceId: string) {
  return useQuery({
    queryKey: ["settings", "hotmart", "jobs", workspaceId],
    enabled: workspaceId.trim().length > 0,
    queryFn: async () => {
      const { data } = await axios.get<HotmartCollectionJobListResponse>("/api/v1/mois/collection-jobs", {
        params: { workspaceId },
      });
      return data.items;
    },
  });
}
