import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type {
  MoisCreateReferencePayload,
  MoisReference,
  MoisReferenceListResponse,
} from "./types";

export function useMoisReferences(workspaceId: string) {
  return useQuery({
    queryKey: ["mois", "references", workspaceId],
    queryFn: async () => {
      const { data } = await axios.get<MoisReferenceListResponse>("/api/v1/mois/references", {
        params: { workspaceId },
      });
      return data.items;
    },
    enabled: workspaceId.trim().length > 0,
  });
}

export function useCreateMoisReference() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (payload: MoisCreateReferencePayload) => {
      const { data } = await axios.post<MoisReference>("/api/v1/mois/references", payload);
      return data;
    },
    onSuccess: async (data) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["mois", "references", data.workspaceId] }),
        queryClient.invalidateQueries({ queryKey: ["mois", "dashboard", data.workspaceId] }),
      ]);
    },
  });
}
