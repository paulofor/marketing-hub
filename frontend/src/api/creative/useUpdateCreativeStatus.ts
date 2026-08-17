import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { Creative } from "./useCreatives";

/** Registra exclusivamente a decisão humana sem reenviar o criativo para edição. */
export function useUpdateCreativeStatus(experimentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, status }: { id: number; status: string }) => {
      const { data } = await axios.patch<Creative>(
        `/api/creatives/${id}/status`,
        { status },
        { timeout: 30000 },
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["creatives", experimentId],
      }),
  });
}
