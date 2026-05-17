import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface ClickbaseCollectedProduct {
  jobId: string;
  referenceId: string;
  title: string;
  productUrl?: string | null;
  producerName?: string | null;
  successScore?: number | null;
  collectedAt?: string | null;
}

interface ClickbaseCollectedProductListResponse {
  workspaceId: string;
  items: ClickbaseCollectedProduct[];
}

export function useClickbaseCollectedProducts(workspaceId: string, limit = 24) {
  return useQuery({
    queryKey: ["settings", "clickbase", "products", workspaceId, limit],
    enabled: workspaceId.trim().length > 0,
    queryFn: async () => {
      const { data } = await axios.get<ClickbaseCollectedProductListResponse>("/api/v1/mois/clickbase/products", {
        params: { workspaceId, limit },
      });
      return data.items;
    },
  });
}


export interface ClickbaseCollectionJob {
  jobId: string;
  workspaceId: string;
  niche?: string | null;
  status: string;
  sources?: string[];
  createdAt?: string | null;
  message?: string | null;
}

interface ClickbaseCollectionJobListResponse {
  items: ClickbaseCollectionJob[];
}

export function useClickbaseCollectionJobs(workspaceId: string) {
  return useQuery({
    queryKey: ["settings", "clickbase", "jobs", workspaceId],
    enabled: workspaceId.trim().length > 0,
    queryFn: async () => {
      const { data } = await axios.get<ClickbaseCollectionJobListResponse>("/api/v1/mois/collection-jobs", {
        params: { workspaceId },
      });
      return data.items;
    },
  });
}
