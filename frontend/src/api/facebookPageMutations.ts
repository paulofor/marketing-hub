import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface UpsertFacebookPage {
  id?: number;
  pageId: string;
  name: string;
}

export function useCreateFacebookPage(accountId: string | number | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (page: UpsertFacebookPage) =>
      axios.post(`/accounts/facebook/${accountId}/pages`, page),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["facebook-pages", accountId] });
      queryClient.invalidateQueries({ queryKey: ["facebook-configuration-status"] });
    },
  });
}

export function useUpdateFacebookPage(accountId: string | number | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (page: UpsertFacebookPage) =>
      axios.put(`/accounts/facebook/${accountId}/pages/${page.id}`, page),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["facebook-pages", accountId] });
    },
  });
}

export function useDeleteFacebookPage(accountId: string | number | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (pageId: number) =>
      axios.delete(`/accounts/facebook/${accountId}/pages/${pageId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["facebook-pages", accountId] });
      queryClient.invalidateQueries({ queryKey: ["facebook-configuration-status"] });
    },
  });
}
