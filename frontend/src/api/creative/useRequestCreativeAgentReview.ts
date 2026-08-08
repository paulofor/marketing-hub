import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Creative } from "./useCreatives";

/** Solicita uma revisão auditável sem alterar o conteúdo ou status humano do criativo. */
export function useRequestCreativeAgentReview(expId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (creativeId: number) => {
      const { data } = await axios.post<Creative>(
        `/api/creatives/${creativeId}/agent-review/request`,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["creatives", expId] });
    },
  });
}
