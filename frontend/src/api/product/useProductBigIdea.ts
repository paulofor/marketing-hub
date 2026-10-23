import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

export interface BigIdeaContent {
  centralIdea: string;
  audience: string;
  desire: string;
  promise: string;
  mechanism: string;
  evidence: string;
  limits: string;
}
export interface BigIdeaRevision {
  id: number;
  productId: number;
  revision: number;
  content: BigIdeaContent;
  changeReason: string;
  createdBy: string;
  sourceTaskId: number | null;
  createdAt: string;
}
export interface BigIdeaWorkspace {
  productId: number;
  status: "DEFINED" | "MISSING";
  current: BigIdeaRevision | null;
  history: BigIdeaRevision[];
  guidance: string;
}

export interface BigIdeaResearch {
  executionId: number | null;
  productId: number;
  status: "NOT_REQUESTED" | "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";
  canRequest: boolean;
  canAdopt: boolean;
  guidance: string;
  proposal: {
    bigIdea: BigIdeaContent;
    selectionReason: string;
    demandAssessment: Record<string, string>;
    alternatives: Array<Record<string, string>>;
    sources: Array<{ url: string; title: string; learning: string }>;
  } | null;
  estimatedCostUsd: number | null;
  modelName: string | null;
}

export function useBigIdeaResearch(productId: number) {
  return useQuery({
    queryKey: ["product-big-idea-research", productId],
    queryFn: async () =>
      (
        await axios.get<BigIdeaResearch>(
          `/api/products/${productId}/big-idea/research`,
        )
      ).data,
    refetchInterval: (query) =>
      ["PENDING", "RUNNING"].includes(query.state.data?.status ?? "")
        ? 5000
        : false,
  });
}

export function useBigIdeaResearchAction(productId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (action: { brief: string } | { executionId: number }) => {
      const base = `/api/products/${productId}/big-idea/research`;
      return (
        "executionId" in action
          ? await axios.post(`${base}/${action.executionId}/adopt`)
          : await axios.post(base, action)
      ).data;
    },
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({
          queryKey: ["product-big-idea-research", productId],
        }),
        client.invalidateQueries({ queryKey: ["product-big-idea", productId] }),
        client.invalidateQueries({ queryKey: ["products"] }),
        client.invalidateQueries({ queryKey: ["product", productId] }),
      ]);
    },
  });
}
export function useProductBigIdea(productId: number) {
  return useQuery({
    queryKey: ["product-big-idea", productId],
    enabled: Number.isSafeInteger(productId) && productId > 0,
    refetchOnWindowFocus: false,
    queryFn: async () =>
      (await axios.get<BigIdeaWorkspace>(`/api/products/${productId}/big-idea`))
        .data,
  });
}
export function useSaveProductBigIdea(productId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (value: {
      expectedRevision: number;
      content: BigIdeaContent;
      changeReason: string;
    }) =>
      (
        await axios.post<BigIdeaRevision>(
          `/api/products/${productId}/big-idea/revisions`,
          value,
        )
      ).data,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ["product-big-idea", productId] }),
        client.invalidateQueries({ queryKey: ["products"] }),
        client.invalidateQueries({ queryKey: ["product", productId] }),
      ]);
    },
  });
}
