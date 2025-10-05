import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
export interface CreateInstagramAccountPayload {
  name: string;
  handle: string;
  code: string;
}

export interface UpdateInstagramAccountPayload
  extends CreateInstagramAccountPayload {
  id: number;
}

export function useCreateInstagramAccount() {
  const queryClient = useQueryClient();
  return useMutation<void, unknown, CreateInstagramAccountPayload>({
    mutationFn: (account: CreateInstagramAccountPayload) =>
      axios.post("/api/accounts/instagram", account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["instagram-accounts"] }),
  });
}

export function useUpdateInstagramAccount() {
  const queryClient = useQueryClient();
  return useMutation<void, unknown, UpdateInstagramAccountPayload>({
    mutationFn: (account: UpdateInstagramAccountPayload) =>
      axios.put(`/api/accounts/instagram/${account.id}`, account),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["instagram-accounts"] }),
  });
}

export function useDeleteInstagramAccount() {
  const queryClient = useQueryClient();
  return useMutation<void, unknown, number>({
    mutationFn: (id: number) =>
      axios.delete(`/api/accounts/instagram/${id}`),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["instagram-accounts"] }),
  });
}
