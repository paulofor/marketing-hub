import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { UpdateCreative } from "./useUpdateCreative";
import type { Creative } from "./useCreatives";

/** Cria uma revisão auditável sem sobrescrever o criativo de origem. */
export function useCreateCreativeVersion(expId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      sourceId,
      ...payload
    }: UpdateCreative & { sourceId: number }) => {
      const { data } = await axios.post<Creative>(
        `/api/creatives/${sourceId}/versions`,
        payload,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["creatives", expId] }),
  });
}
