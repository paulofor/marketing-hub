import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Creative } from "./useCreatives";

export interface UpdateCreativeLabels {
  angles: number[];
  visualProofs: number[];
  emotionalTriggers: number[];
}

export interface UpdateCreativeLabelsVariables {
  id: number;
  labels: UpdateCreativeLabels;
}

export function useUpdateCreativeLabels(expId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, labels }: UpdateCreativeLabelsVariables) => {
      const { data: creative } = await axios.patch<Creative>(
        `/api/creatives/${id}/labels`,
        labels,
      );
      return creative;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["creatives", expId] });
    },
  });
}
