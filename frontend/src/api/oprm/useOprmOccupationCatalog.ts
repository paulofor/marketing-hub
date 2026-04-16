import { useMutation, useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface OprmOccupationCatalogItem {
  id: string;
  occupationSeedRef: string;
  displayName: string;
  aliases: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface OprmOccupationCatalogUpsertRequest {
  occupationSeedRef: string;
  displayName: string;
  aliases: string[];
  active: boolean;
}

export function useOprmOccupationCatalog() {
  return useQuery({
    queryKey: ["oprm", "occupation-catalog"],
    queryFn: async () => {
      const { data } = await axios.get<OprmOccupationCatalogItem[]>(
        "/api/oprm/occupations",
      );
      return data;
    },
  });
}

export function useCreateOprmOccupation() {
  return useMutation({
    mutationFn: async (request: OprmOccupationCatalogUpsertRequest) => {
      const { data } = await axios.post<OprmOccupationCatalogItem>(
        "/api/oprm/occupations",
        request,
      );
      return data;
    },
  });
}

export function useUpdateOprmOccupation() {
  return useMutation({
    mutationFn: async ({
      occupationId,
      request,
    }: {
      occupationId: string;
      request: OprmOccupationCatalogUpsertRequest;
    }) => {
      const { data } = await axios.put<OprmOccupationCatalogItem>(
        `/api/oprm/occupations/${occupationId}`,
        request,
      );
      return data;
    },
  });
}

export function useDeleteOprmOccupation() {
  return useMutation({
    mutationFn: async (occupationId: string) => {
      await axios.delete(`/api/oprm/occupations/${occupationId}`);
    },
  });
}
