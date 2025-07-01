import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { FacebookAccount } from "./useFacebookAccounts";

export function useCreateFacebookAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: FacebookAccount) =>
      axios.post("/api/accounts/facebook", account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
  });
}

export function useUpdateFacebookAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: FacebookAccount) =>
      axios.put(`/api/accounts/facebook/${account.id}`, account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
  });
}

export function useDeleteFacebookAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => axios.delete(`/api/accounts/facebook/${id}`),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
  });
}
