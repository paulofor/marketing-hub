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
