import axios from "axios";
import { useQuery } from "@tanstack/react-query";

export type ProductInPlay = {
  productId: number;
  productName: string;
  commercialName: string | null;
  chainId: number | null;
  cycleId: number | null;
  sourceReference: string | null;
  processDefinitionId: number | null;
  runId: number | null;
  activityId: string | null;
  activityNumber: string | null;
  activityName: string;
  responsible: string | null;
  status: string;
  statusLabel: string;
  reason: string | null;
  activityUrl: string;
  lastReconciledAt: string | null;
};

export type ProductsInPlay = {
  observedAt: string;
  total: number;
  executing: number;
  waiting: number;
  blocked: number;
  paused: number;
  unavailable: number;
  products: ProductInPlay[];
};

/** Acompanha a fotografia oficial sem iniciar tarefas nem recalcular estados. */
export function useProductsInPlay() {
  return useQuery({
    queryKey: ["products", "operations", "in-play"],
    queryFn: async ({ signal }) =>
      (
        await axios.get<ProductsInPlay>("/api/products/operations/v1/in-play", {
          signal,
          timeout: 45000,
        })
      ).data,
    refetchInterval: 30000,
    refetchIntervalInBackground: false,
    retry: 1,
  });
}
