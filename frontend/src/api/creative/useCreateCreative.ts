import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Creative } from "./useCreatives";

export interface CreateCreative {
  headline: string;
  primaryText: string;
  imageUrl: string;
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
