import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Creative } from "./useCreatives";

export interface CreateCreative {
  format: string;
  primaryText: string;
  headline: string;
  description?: string;
  cta?: string;
  url?: string;
  imageUrl?: string;
  videoId?: string;
  pageId?: string;
  instagramUserId?: string;
  status: string;
}

export function useCreateCreative(expId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateCreative) => {
      const { data: creative } = await axios.post<Creative>(
        `/api/experiments/${expId}/creatives`,
        data,
      );
      return creative;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["creatives", expId] });
    },
  });
}
