import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ProcessDiagram } from "../businessProcess/types";
import { z } from "zod";

export const cycleApi = "/api/business-process-chains/learning-cycles/v1";
export type LearningCycleEntry = {
  chainDefinitionId: number;
  chainName: string;
  parentProcessDefinitionId: number;
  parentProcessName: string;
  sequenceNumber: number;
  activityId?: string | null;
  processDefinitionId: number;
  processName: string;
  integrated: boolean;
  canStartCycle: boolean;
  guidance: string;
  workspaceUrl: string;
  actionLabel: string;
  parentUrl: string;
  returnRoutes: {
    label: string;
    condition: string;
    processDefinitionId: number;
    sequenceNumber: number;
    processName: string;
    url: string;
  }[];
};
const entrySchema = z.object({
  chainDefinitionId: z.number().int().positive(),
  chainName: z.string(),
  parentProcessDefinitionId: z.number().int().positive(),
  parentProcessName: z.string(),
  sequenceNumber: z.number().int().positive(),
  activityId: z.string().nullish(),
  processDefinitionId: z.number().int().positive(),
  processName: z.string(),
  integrated: z.boolean(),
  canStartCycle: z.boolean(),
  guidance: z.string(),
  workspaceUrl: z
    .string()
    .startsWith("/business-process-chains/learning-cycles?"),
  actionLabel: z.string(),
  parentUrl: z.string().startsWith("/"),
  returnRoutes: z.array(
    z.object({
      label: z.string(),
      condition: z.string(),
      processDefinitionId: z.number().int().positive(),
      sequenceNumber: z.number().int().positive(),
      processName: z.string(),
      url: z.string().startsWith("/"),
    }),
  ),
});
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
  diagram?: ProcessDiagram;
  brief: Record<string, unknown>;
  inheritedLearning: {
    cycleId?: number;
    experimentId?: number;
    productVersion?: string;
    events?: CycleEvent[];
  };
  events: CycleEvent[];
  approvalOptions?: { id: number; label: string }[];
  evidenceOptions?: Record<string, { id: number; label: string }[]>;
  workLinks?: { label: string; url: string }[];
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
  entry?: LearningCycleEntry;
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
export function useLearningCycleEntry(
  processDefinitionId?: number,
  productId?: number,
  chainId?: number,
) {
  return useQuery({
    queryKey: ["learning-cycle-entry", processDefinitionId, productId, chainId],
    enabled: !!processDefinitionId,
    queryFn: async () => {
      const { data } = await axios.get<unknown>(`${cycleApi}/entry`, {
        params: { processDefinitionId, productId, chainId },
      });
      return entrySchema.nullable().parse(data === "" ? null : data);
    },
  });
}
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
        client.invalidateQueries({ queryKey: ["learning-cycle-entry"] }),
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
