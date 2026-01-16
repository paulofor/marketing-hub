import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export function useDeletePromptDomain() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => {
      await axios.delete(`/api/prompt-domains/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["prompt-domains"] });
    },
  });
}
