import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
export interface FacebookAccountPayload {
  id?: number;
  name: string;
  currency: string;
}

export function useCreateFacebookAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: FacebookAccountPayload) =>
      axios.post("/accounts/facebook", account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
  });
}

export function useUpdateFacebookAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (account: FacebookAccountPayload) =>
      axios.put(`/accounts/facebook/${account.id}`, account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
  });
}

export function useDeleteFacebookAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => axios.delete(`/accounts/facebook/${id}`),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["facebook-accounts"] }),
  });
}
