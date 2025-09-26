import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { InstagramAccount } from "./useInstagramAccounts";

export function useCreateInstagramAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: InstagramAccount) =>
      axios.post("/accounts/instagram", account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["instagram-accounts"] }),
  });
}

export function useUpdateInstagramAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: InstagramAccount) =>
      axios.put(`/accounts/instagram/${account.id}`, account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["instagram-accounts"] }),
  });
}

export function useDeleteInstagramAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => axios.delete(`/accounts/instagram/${id}`),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["instagram-accounts"] }),
  });
}
