import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface ProcessAutomation {
  id: number | null;
  productId: number;
  processDefinitionId: number;
  chainId: number;
  learningCycleId: number | null;
  sourceReference: string;
  status: string;
  reason: string;
  currentActivityId: string | null;
  currentActivityName: string | null;
  currentOwnerName: string | null;
  currentSequence: number | null;
  totalActivities: number;
  completedActivities: number;
  remainingActivities: number;
  omittedActivities: number;
  completionPercentage: number;
  knownCostUsd: number | null;
  costCoverage: string | null;
  canStart: boolean;
  canPause: boolean;
  canResume: boolean;
  automaticExecution: boolean;
  childRunId: number | null;
  navigationUrl: string | null;
  lastReconciledAt: string | null;
  updatedAt: string | null;
  revision: number;
}

export interface ProcessAutomationEvent {
  id: number;
  eventType: string;
  status: string;
  activityId: string | null;
  message: string;
  details: Record<string, unknown>;
  createdAt: string;
}

/** Acompanha somente a projeção persistida; nenhuma atualização dispara a próxima atividade. */
export function useProcessAutomation(
  productId: number,
  processId: number,
  chainId?: number,
  learningCycleId?: number,
  sourceReference?: string | null,
) {
  const client = useQueryClient();
  const root = `/api/business-processes/${processId}/products/${productId}/automation/v1`;
  const queryKey = [
    "process-automation",
    productId,
    processId,
    chainId,
    learningCycleId,
    sourceReference,
  ];
  const context = { chainId, learningCycleId, sourceReference };
  const status = useQuery({
    queryKey,
    enabled: Boolean(chainId && sourceReference),
    queryFn: async ({ signal }) =>
      (
        await axios.get<ProcessAutomation>(root, {
          params: context,
          signal,
          timeout: 45000,
        })
      ).data,
    refetchInterval: 3000,
    retry: 1,
  });
  const command = useMutation({
    mutationFn: async (action: "start" | "pause" | "resume") => {
      if (action !== "start" && !status.data?.id)
        throw new Error("Execução não identificada.");
      return (
        await axios.post<ProcessAutomation>(
          action === "start" ? root : `${root}/${status.data!.id}/${action}`,
          action === "start" ? context : undefined,
          { timeout: 45000 },
        )
      ).data;
    },
    onSuccess: (data) => {
      client.setQueryData(queryKey, data);
      void client.invalidateQueries({
        queryKey: [
          "products",
          productId,
          "business-processes",
          processId,
          "activity-executions",
        ],
      });
      void client.invalidateQueries({
        queryKey: ["process-automation-events", data.id],
      });
    },
  });
  return { status, command, root };
}

/** Lê o diário sob demanda e preserva a paginação fornecida pelo backend. */
export function useProcessAutomationEvents(
  root: string,
  runId: number | null | undefined,
  enabled: boolean,
  beforeId?: number,
) {
  return useQuery({
    queryKey: ["process-automation-events", runId, beforeId],
    enabled: Boolean(enabled && runId),
    queryFn: async ({ signal }) =>
      (
        await axios.get<ProcessAutomationEvent[]>(`${root}/${runId}/events`, {
          params: { beforeId },
          signal,
          timeout: 15000,
        })
      ).data,
    retry: 1,
  });
}
