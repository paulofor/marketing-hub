import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type ProductProcessCommit = {
  id: number;
  productId: number;
  processDefinitionId: number;
  processCode: string;
  processName: string;
  processVersion: number;
  repositoryName: string;
  commitSha: string;
  commitSummary: string;
  commitUrl?: string | null;
  recordedBy: string;
  recordedAt: string;
};

export type RegisterProductProcessCommit = {
  processDefinitionId: number;
  repositoryName: string;
  commitSha: string;
  commitSummary: string;
  commitUrl?: string | null;
  recordedBy: string;
};

export function useProductProcessCommits(productId?: string | number) {
  const normalizedProductId =
    productId == null ? undefined : String(productId).trim();
  return useQuery({
    queryKey: ["products", "process-commits", normalizedProductId],
    enabled: Boolean(normalizedProductId),
    queryFn: async () =>
      (
        await axios.get<ProductProcessCommit[]>(
          `/api/products/${normalizedProductId}/process-commits`,
        )
      ).data,
  });
}

export function useRegisterProductProcessCommit(productId?: string | number) {
  const queryClient = useQueryClient();
  const normalizedProductId =
    productId == null ? undefined : String(productId).trim();
  return useMutation({
    mutationFn: async (payload: RegisterProductProcessCommit) =>
      (
        await axios.post<ProductProcessCommit>(
          `/api/products/${normalizedProductId}/process-commits`,
          payload,
        )
      ).data,
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["products", "process-commits", normalizedProductId],
      }),
  });
}
