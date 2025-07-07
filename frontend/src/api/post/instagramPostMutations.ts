import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { InstagramPost } from "./useInstagramPosts";

export function useCreateInstagramPost(accountId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (post: InstagramPost) =>
      axios.post(`/api/accounts/instagram/${accountId}/posts`, post),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["instagram-posts", accountId],
      }),
  });
}

export function useUpdateInstagramPost(accountId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (post: InstagramPost) =>
      axios.put(`/api/accounts/instagram/${accountId}/posts/${post.id}`, post),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["instagram-posts", accountId],
      }),
  });
}

export function useDeleteInstagramPost(accountId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) =>
      axios.delete(`/api/accounts/instagram/${accountId}/posts/${id}`),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["instagram-posts", accountId],
      }),
  });
}
