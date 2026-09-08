import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ProcessDiagram } from "../businessProcess/types";

export const cycleApi = "/api/business-process-chains/learning-cycles/v1";
export type CycleEvent = {
  id: number;
  revision: number;
  action: string;
  fromStage: string;
  toStage: string;
  operatorName: string;
  summary: string;
  evidenceReference: string;
  evidence: Record<string, unknown>;
  createdAt: string;
};
export type LearningCycle = {
  id: number;
  productId: number;
  experimentId: number;
  previousCycleId?: number;
  successorCycleId?: number;
  chainDefinitionId: number;
  processDefinitionId: number;
  revision: number;
  stage: string;
  stageLabel: string;
  status: string;
  baseline: boolean;
  productVersion: string;
  budgetLimitBrl: number;
  windowStart: string;
  windowEnd: string;
  nextAction: string;
  responsible: string;
  returnProcessId?: number;
  returnActivityId?: string;
  workUrl: string;
  brief: Record<string, unknown>;
  inheritedLearning: {
    cycleId?: number;
    experimentId?: number;
    productVersion?: string;
    events?: CycleEvent[];
  };
  events: CycleEvent[];
  approvalOptions?: { id: number; label: string }[];
  commands: {
    action: string;
    label: string;
    available: boolean;
    reason: string;
  }[];
  canCreateSuccessor: boolean;
  createdAt: string;
  closedAt?: string;
};
export type CycleCatalog = {
  processDefinitionId: number;
  version: number;
  diagram: ProcessDiagram;
  returnTargets: {
    processDefinitionId: number;
    processName: string;
    activityId: string;
    activityName: string;
    owner: string;
    processCode: string;
  }[];
  experiments: {
    id: number;
    name: string;
    status: string;
    available: boolean;
    baseline: boolean;
    reason: string;
  }[];
};
export function useCycleCatalog(chainId?: number, productId?: number) {
  return useQuery({
    queryKey: ["learning-cycle-catalog", chainId, productId],
    enabled: !!chainId,
    queryFn: async () =>
      (
        await axios.get<CycleCatalog>(`${cycleApi}/catalog`, {
          params: { chainId, productId },
        })
      ).data,
  });
}
export function useLearningCycles(productId?: number, chainId?: number) {
  return useQuery({
    queryKey: ["learning-cycles", productId, chainId],
    enabled: !!productId,
    queryFn: async () =>
      (
        await axios.get<LearningCycle[]>(`${cycleApi}/products/${productId}`, {
          params: { chainId },
        })
      ).data,
  });
}
export function useCycleMutation(productId?: number, cycleId?: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (body: Record<string, unknown>) =>
      (
        await axios.post<LearningCycle>(
          `${cycleApi}/products/${productId}${cycleId ? `/${cycleId}/commands` : ""}`,
          body,
        )
      ).data,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ["learning-cycles"] }),
        client.invalidateQueries({ queryKey: ["learning-cycle-catalog"] }),
        client.invalidateQueries({
          queryKey: ["products", productId, "business-processes"],
        }),
      ]);
    },
  });
}
export function cycleError(error: unknown) {
  if (axios.isAxiosError(error))
    return (
      error.response?.data?.detail ||
      error.response?.data?.message ||
      error.response?.data?.reason ||
      "Não foi possível registrar. Atualize a leitura e confira os requisitos."
    );
  return "Não foi possível registrar a decisão. Tente novamente após atualizar a leitura.";
}
