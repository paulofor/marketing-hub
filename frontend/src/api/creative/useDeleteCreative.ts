import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useDeleteCreative(id: number, expId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await axios.delete(`/api/creatives/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["creatives", expId] });
    },
  });
}
