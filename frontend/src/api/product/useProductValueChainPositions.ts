import { useQuery } from "@tanstack/react-query";
import axios from "axios";

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
  subprocessPosition?: {
    trackingStatus: "NOT_APPLICABLE" | "PLANNED" | "IN_PROGRESS" | "RECORDED";
    subprocessCount: number;
    currentActivityName?: string | null;
    currentSubprocessDefinitionId?: number | null;
    currentSubprocessCode?: string | null;
    currentSubprocessName?: string | null;
    currentSubprocessObjective?: string | null;
    nextSubprocessDefinitionId?: number | null;
    nextSubprocessCode?: string | null;
    nextSubprocessName?: string | null;
    nextSubprocessObjective?: string | null;
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
