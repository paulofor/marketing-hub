import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useDeleteDifferentiatedTechnology() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/api/differentiated-technologies/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["differentiatedTechnologies"] });
    },
  });
}
