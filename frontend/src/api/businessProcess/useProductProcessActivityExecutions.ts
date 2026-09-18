import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { useEffect, useRef } from "react";
import type {
  BusinessProcessActivityExecution,
  ProductProcessActivityExecutionHistory,
  ProductProcessActivityHumanDecision,
  ProductProcessActivityExecutionRequest,
} from "./types";

export type ProductProcessActivityExecutionCommand = {
  activityId: string;
  decision?: ProductProcessActivityHumanDecision;
};

/** Lê a orientação da referência escolhida, sem iniciar acompanhamento periódico de tarefas. */
export function useProductProcessActivityHistory(
  productId?: number,
  processDefinitionId?: number,
  learningCycleId?: number,
  chainId?: number,
  requestedReference?: string,
) {
  const query = new URLSearchParams();
  if (learningCycleId) query.set("learningCycleId", String(learningCycleId));
  if (chainId) query.set("chainId", String(chainId));
  if (requestedReference) query.set("sourceReference", requestedReference);
  const queryString = query.toString();
  return useQuery({
    queryKey: [
      "products",
      productId,
      "business-processes",
      processDefinitionId,
      "activity-executions",
      learningCycleId,
      chainId,
      ...(requestedReference ? [requestedReference] : []),
    ],
    enabled: Boolean(productId && processDefinitionId),
    queryFn: async ({ signal }) =>
      (
        await axios.get<ProductProcessActivityExecutionHistory>(
          `/api/business-processes/${processDefinitionId}/products/${productId}/activity-executions${queryString ? `?${queryString}` : ""}`,
          { signal, timeout: 45000, params: { includePromptAudit: false } },
        )
      ).data,
  });
}

/** Lê a auditoria extensa apenas quando a pessoa abre uma tarefa da lista resumida. */
export function useProductProcessTaskAudit(url?: string, enabled = false) {
  return useQuery({
    queryKey: ["product-process-task-audit", url],
    enabled: Boolean(url && enabled),
    retry: 1,
    staleTime: 30_000,
    queryFn: async ({ signal }) =>
      (
        await axios.get<BusinessProcessActivityExecution>(url!, {
          signal,
          timeout: 45_000,
        })
      ).data,
  });
}

/** Consulta o contexto exato e acompanha revisões das tarefas do processo selecionado. */
export function useProductProcessActivityExecutions(
  productId?: number,
  processDefinitionId?: number,
  learningCycleId?: number,
  chainId?: number,
  requestedReference?: string,
) {
  const queryClient = useQueryClient();
  const previousProgress = useRef<string | undefined>(undefined);
  const lastRefreshTick = useRef<string | undefined>(undefined);
  const history = useProductProcessActivityHistory(
    productId,
    processDefinitionId,
    learningCycleId,
    chainId,
    requestedReference,
  );
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
        ...(requestedReference ? [requestedReference] : []),
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
    requestedReference,
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

/** Solicita as tarefas da atividade no mesmo contexto exibido na tela. */
export function useRequestProductProcessActivityExecution(
  productId: number,
  processDefinitionId: number,
  learningCycleId?: number,
  requestedReference?: string,
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      activityId,
      decision,
    }: ProductProcessActivityExecutionCommand) => {
      const query = new URLSearchParams();
      if (learningCycleId)
        query.set("learningCycleId", String(learningCycleId));
      if (requestedReference) query.set("sourceReference", requestedReference);
      const queryString = query.toString();
      const url = `/api/business-processes/${processDefinitionId}/products/${productId}/activities/${activityId}/execution-requests${queryString ? `?${queryString}` : ""}`;
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
