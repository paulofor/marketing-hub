import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { useEffect, useRef } from "react";
import type {
  ProductProcessActivityExecutionHistory,
  ProductProcessActivityHumanDecision,
  ProductProcessActivityExecutionRequest,
} from "./types";

export type ProductProcessActivityExecutionCommand = {
  activityId: string;
  decision?: ProductProcessActivityHumanDecision;
};

/** Consulta as atividades e tarefas auditáveis de um produto no processo selecionado. */
export function useProductProcessActivityExecutions(
  productId?: number,
  processDefinitionId?: number,
  learningCycleId?: number,
  chainId?: number,
) {
  const queryClient = useQueryClient();
  const previousProgress = useRef<string | undefined>(undefined);
  const lastRefreshTick = useRef<string | undefined>(undefined);
  const query = new URLSearchParams();
  if (learningCycleId) query.set("learningCycleId", String(learningCycleId));
  if (chainId) query.set("chainId", String(chainId));
  const queryString = query.toString();
  const history = useQuery({
    queryKey: [
      "products",
      productId,
      "business-processes",
      processDefinitionId,
      "activity-executions",
      learningCycleId,
      chainId,
    ],
    enabled: Boolean(productId && processDefinitionId),
    queryFn: async ({ signal }) =>
      (
        await axios.get<ProductProcessActivityExecutionHistory>(
          `/api/business-processes/${processDefinitionId}/products/${productId}/activity-executions${queryString ? `?${queryString}` : ""}`,
          { signal, timeout: 45000 },
        )
      ).data,
  });
  const sourceReference = history.data?.currentExecutionReference;
  const progress = useQuery({
    queryKey: [
      "product-process-progress",
      productId,
      processDefinitionId,
      sourceReference,
    ],
    enabled: Boolean(productId && processDefinitionId && sourceReference),
    queryFn: async ({ signal }) =>
      (
        await axios.get<
          { taskId: number; status: string; updatedAt: string }[]
        >(
          `/api/business-processes/${processDefinitionId}/products/${productId}/execution-progress`,
          { params: { sourceReference }, signal, timeout: 15000 },
        )
      ).data,
    refetchInterval: 3000,
    retry: 1,
  });
  useEffect(() => {
    if (!progress.data) return;
    const revision = JSON.stringify([
      productId,
      processDefinitionId,
      sourceReference,
      progress.data,
    ]);
    const tick = JSON.stringify([
      productId,
      processDefinitionId,
      sourceReference,
      progress.dataUpdatedAt,
    ]);
    if (
      history.isFetching ||
      lastRefreshTick.current === tick ||
      (previousProgress.current === revision && !history.isRefetchError)
    )
      return;
    previousProgress.current = revision;
    lastRefreshTick.current = tick;
    void queryClient.invalidateQueries({
      queryKey: [
        "products",
        productId,
        "business-processes",
        processDefinitionId,
        "activity-executions",
        learningCycleId,
        chainId,
      ],
    });
    void queryClient.invalidateQueries({
      queryKey: ["cycle-process-context", productId],
    });
  }, [
    progress.data,
    progress.dataUpdatedAt,
    history.isFetching,
    history.isRefetchError,
    queryClient,
    productId,
    processDefinitionId,
    sourceReference,
    learningCycleId,
    chainId,
  ]);
  useEffect(() => {
    if (!progress.dataUpdatedAt) return;
    const failedContexts = queryClient
      .getQueryCache()
      .findAll({ queryKey: ["cycle-process-context", productId] })
      .filter(
        (query) =>
          query.state.status === "error" && query.state.fetchStatus === "idle",
      );
    for (const query of failedContexts)
      void queryClient.invalidateQueries({
        queryKey: query.queryKey,
        exact: true,
      });
  }, [progress.dataUpdatedAt, productId, queryClient]);
  return { ...history, trackingError: progress.isError };
}

/** Solicita ao backend todas as tarefas responsáveis pela atividade do produto. */
export function useRequestProductProcessActivityExecution(
  productId: number,
  processDefinitionId: number,
  learningCycleId?: number,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      activityId,
      decision,
    }: ProductProcessActivityExecutionCommand) => {
      const url = `/api/business-processes/${processDefinitionId}/products/${productId}/activities/${activityId}/execution-requests${learningCycleId ? `?learningCycleId=${learningCycleId}` : ""}`;
      return decision
        ? (
            await axios.post<ProductProcessActivityExecutionRequest>(
              url,
              decision,
            )
          ).data
        : (await axios.post<ProductProcessActivityExecutionRequest>(url)).data;
    },
    onSuccess: () => {
      void Promise.all([
        queryClient.invalidateQueries({
          queryKey: [
            "product-process-progress",
            productId,
            processDefinitionId,
          ],
        }),
        queryClient.invalidateQueries({
          queryKey: ["cycle-process-context", productId],
        }),
        queryClient.invalidateQueries({
          queryKey: [
            "products",
            productId,
            "business-processes",
            processDefinitionId,
            "activity-executions",
          ],
        }),
        queryClient.invalidateQueries({
          queryKey: ["products", "value-chain-positions"],
        }),
        queryClient.invalidateQueries({ queryKey: ["experiment-runs"] }),
        queryClient.invalidateQueries({
          queryKey: ["experiment-run-preflight"],
        }),
      ]);
    },
  });
}
