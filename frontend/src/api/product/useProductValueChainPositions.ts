import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ProductStageMeasurement = {
  stageType: "PROCESS" | "SUBPROCESS";
  sequenceLabel?: string | null;
  trackingStatus: "CURRENT" | "COMPLETED" | "RECORDED";
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
    trackingStatus: "NOT_APPLICABLE" | "PLANNED" | "IN_PROGRESS" | "RECORDED";
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

export function useProductValueChainPositions() {
  return useQuery({
    queryKey: ["products", "value-chain-positions"],
    queryFn: async () =>
      (
        await axios.get<ProductValueChainPosition[]>(
          "/api/products/value-chain-positions",
        )
      ).data,
  });
}

export function useProductValueChainPosition(productId?: string | number) {
  return useQuery({
    queryKey: ["products", "value-chain-positions", productId],
    enabled: Boolean(productId),
    queryFn: async () =>
      (
        await axios.get<ProductValueChainPosition>(
          `/api/products/value-chain-positions/${productId}`,
        )
      ).data,
  });
}
