import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useDeleteCreative(expId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/api/creatives/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["creatives", expId] });
    },
  });
}
