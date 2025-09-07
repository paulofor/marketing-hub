import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Creative } from "./useCreatives";

export interface UpdateCreative {
  headline: string;
  primaryText: string;
  imageUrl: string;
  status: string;
}

export interface UpdateCreativePayload extends UpdateCreative {
  id: number;
}

export function useUpdateCreative(expId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, ...data }: UpdateCreativePayload) => {
      const { data: creative } = await axios.put<Creative>(
        `/api/creatives/${id}`,
        data,
      );
      return creative;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["creatives", expId] });
    },
  });
}
