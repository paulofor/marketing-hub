import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type {
  BusinessProcessChainDetail,
  BusinessProcessChainSummary,
} from "./types";

const key = ["business-process-chains"];

export function useBusinessProcessChains() {
  return useQuery({
    queryKey: key,
    queryFn: async () =>
      (
        await axios.get<BusinessProcessChainSummary[]>(
          "/api/business-process-chains",
        )
      ).data,
  });
}

export function useBusinessProcessChain(id?: number) {
  return useQuery({
    queryKey: [...key, id],
    enabled: id !== undefined,
    queryFn: async () =>
      (
        await axios.get<BusinessProcessChainDetail>(
          `/api/business-process-chains/${id}`,
        )
      ).data,
  });
}

export function useBusinessProcessChainsByProcess(
  processDefinitionId?: number,
) {
  return useQuery({
    queryKey: [...key, "by-process", processDefinitionId],
    enabled: processDefinitionId !== undefined,
    queryFn: async () =>
      (
        await axios.get<BusinessProcessChainSummary[]>(
          `/api/business-process-chains/by-process/${processDefinitionId}`,
        )
      ).data,
  });
}
