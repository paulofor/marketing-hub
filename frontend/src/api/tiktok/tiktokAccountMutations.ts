import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface TiktokAccountPayload {
  id?: number;
  name: string;
  advertiserId: string;
  accessToken?: string | null;
  appId?: string | null;
  clientKey?: string | null;
  appSecret?: string | null;
  metricsEnabled: boolean;
  publicationEnabled: boolean;
}

export interface TiktokDiagnosticResponse {
  accountId: number;
  status: string;
  message: string;
  checks: string[];
  blockers: string[];
  checkedAt: string;
}

export function useCreateTiktokAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: TiktokAccountPayload) =>
      axios.post("/api/tiktok/accounts", account),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tiktok-accounts"] });
    },
  });
}

export function useUpdateTiktokAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: TiktokAccountPayload) =>
      axios.put(`/api/tiktok/accounts/${account.id}`, account),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tiktok-accounts"] });
    },
  });
}

export function useDeleteTiktokAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => axios.delete(`/api/tiktok/accounts/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tiktok-accounts"] });
    },
  });
}

export function useDiagnoseTiktokAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      const { data } = await axios.post<TiktokDiagnosticResponse>(
        `/api/tiktok/accounts/${id}/diagnostics`,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tiktok-accounts"] });
    },
  });
}
