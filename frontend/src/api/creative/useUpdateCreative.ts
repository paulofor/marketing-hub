import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Creative } from "./useCreatives";

export interface UpdateCreative {
  headline: string;
  primaryText: string;
  imageUrl: string;
  status: string;
}

export function useUpdateCreative(id: number, expId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: UpdateCreative) => {
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
