import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  BusinessProcessChainDetail,
  BusinessProcessChainSummary,
  BusinessProcessChainSave,
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

export function useBusinessProcessChainCatalog() {
  return useQuery({
    queryKey: [...key, "catalog"],
    queryFn: async () =>
      (
        await axios.get<BusinessProcessChainSummary[]>(
          "/api/business-process-chains/catalog",
        )
      ).data,
  });
}

export function useSaveBusinessProcessChain() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async ({
      id,
      value,
    }: {
      id: number;
      value: BusinessProcessChainSave;
    }) =>
      (
        await axios.put<BusinessProcessChainDetail>(
          `/api/business-process-chains/${id}`,
          value,
        )
      ).data,
    onSuccess: () => client.invalidateQueries({ queryKey: key }),
  });
}

export function useCreateBusinessProcessChainDraft() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>
      (
        await axios.post<BusinessProcessChainDetail>(
          `/api/business-process-chains/${id}/draft`,
        )
      ).data,
    onSuccess: () => client.invalidateQueries({ queryKey: key }),
  });
}

export function usePublishBusinessProcessChain() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>
      (
        await axios.post<BusinessProcessChainDetail>(
          `/api/business-process-chains/${id}/publish`,
        )
      ).data,
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: key });
      await client.invalidateQueries({ queryKey: ["learning-cycles"] });
      await client.invalidateQueries({ queryKey: ["learning-cycle-catalog"] });
    },
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
