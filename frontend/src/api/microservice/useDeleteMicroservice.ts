import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useDeleteMicroservice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/api/microservices/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["microservices"] });
    },
  });
}
