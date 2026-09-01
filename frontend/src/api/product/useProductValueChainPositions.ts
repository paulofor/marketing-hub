import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ProductStageMeasurement = {
  stageType: "PROCESS" | "SUBPROCESS";
  sequenceLabel?: string | null;
  trackingStatus: "CURRENT" | "COMPLETED" | "RECORDED" | "PLANNED";
  processDefinitionId: number;
  processCode: string;
  processName: string;
  enteredAt?: string | null;
  entryEvidence: string;
  exitedAt?: string | null;
  exitEvidence?: string | null;
  objectiveAchieved: boolean;
  elapsedDays?: number | null;
  knownEstimatedCostUsd: number;
  costCoverage: "COMPLETE" | "PARTIAL" | "NOT_REPORTED" | "NO_EXECUTIONS";
  costedExecutionCount: number;
  uncostedExecutionCount: number;
  commitRegistrationAllowed: boolean;
};

export type ProductValueChainPosition = {
  productId: number;
  commercialStatus?: string | null;
  resolutionStatus: "IDENTIFIED" | "NOT_IDENTIFIED" | "CHAIN_UNAVAILABLE";
  resolutionMessage: string;
  chainDefinitionId?: number | null;
  chainName?: string | null;
  chainVersion?: number | null;
  processDefinitionId?: number | null;
  processCode?: string | null;
  processName?: string | null;
  processVersion?: number | null;
  sequenceNumber?: number | null;
  processCount?: number | null;
  processMeasurements?: ProductStageMeasurement[];
  subprocessPosition?: {
    trackingStatus:
      "NOT_APPLICABLE" | "PLANNED" | "IN_PROGRESS" | "RECORDED" | "COMPLETED";
    subprocessCount: number;
    currentActivityName?: string | null;
    currentSubprocessDefinitionId?: number | null;
    currentSubprocessSequenceNumber?: number | null;
    currentSubprocessCode?: string | null;
    currentSubprocessName?: string | null;
    currentSubprocessObjective?: string | null;
    nextSubprocessDefinitionId?: number | null;
    nextSubprocessSequenceNumber?: number | null;
    nextSubprocessCode?: string | null;
    nextSubprocessName?: string | null;
    nextSubprocessObjective?: string | null;
    measurements?: ProductStageMeasurement[];
  } | null;
};

export type ProductValueChainSummary = {
  productId: number;
  productName?: string | null;
  productInternalName?: string | null;
  commercialStatus?: string | null;
  resolutionStatus: "IDENTIFIED" | "NOT_IDENTIFIED" | "CHAIN_UNAVAILABLE";
  resolutionMessage: string;
  chainDefinitionId?: number | null;
  chainName?: string | null;
  chainVersion?: number | null;
  processDefinitionId?: number | null;
  processCode?: string | null;
  processName?: string | null;
  processVersion?: number | null;
  sequenceNumber?: number | null;
  processCount?: number | null;
};

export function sortProductStageMeasurements(
  first: ProductStageMeasurement,
  second: ProductStageMeasurement,
) {
  const firstSequence = first.sequenceLabel
    ?.split(".")
    .map((part) => Number(part));
  const secondSequence = second.sequenceLabel
    ?.split(".")
    .map((part) => Number(part));
  if (
    firstSequence?.every(Number.isFinite) &&
    secondSequence?.every(Number.isFinite)
  ) {
    const parts = Math.max(firstSequence.length, secondSequence.length);
    for (let index = 0; index < parts; index += 1) {
      if (firstSequence[index] == null) return -1;
      if (secondSequence[index] == null) return 1;
      if (firstSequence[index] !== secondSequence[index]) {
        return firstSequence[index] - secondSequence[index];
      }
    }
  }
  if (first.enteredAt && second.enteredAt) {
    return Date.parse(first.enteredAt) - Date.parse(second.enteredAt);
  }
  if (first.enteredAt) return -1;
  if (second.enteredAt) return 1;
  return first.processDefinitionId - second.processDefinitionId;
}

export function useProductValueChainPositions(playOnly = false) {
  return useQuery({
    queryKey: ["products", "value-chain-positions", { playOnly }],
    queryFn: async () =>
      (
        await axios.get<ProductValueChainPosition[]>(
          "/api/products/value-chain-positions",
          playOnly ? { params: { playOnly: true } } : undefined,
        )
      ).data,
  });
}

export function useProductValueChainPosition(
  productId?: string | number,
  enabled = true,
) {
  return useQuery({
    queryKey: ["products", "value-chain-positions", productId],
    enabled: Boolean(productId) && enabled,
    queryFn: async () =>
      (
        await axios.get<ProductValueChainPosition>(
          `/api/products/value-chain-positions/${productId}`,
        )
      ).data,
  });
}

export function useProductValueChainSummary(productId?: string | number) {
  return useQuery({
    queryKey: ["products", "value-chain-summary", productId],
    enabled: Boolean(productId),
    queryFn: async () =>
      (
        await axios.get<ProductValueChainSummary>(
          `/api/products/value-chain-positions/${productId}/summary`,
        )
      ).data,
  });
}
