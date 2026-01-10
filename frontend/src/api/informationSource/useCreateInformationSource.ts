import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import type { InformationSource } from "./types";

export interface CreateInformationSourcePayload {
  name: string;
  url: string;
}

export function useCreateInformationSource(nicheId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateInformationSourcePayload) => {
      const { data } = await axios.post<InformationSource>(
        `/api/niches/${nicheId}/information-sources`,
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["information-sources", "niche", nicheId],
      });
    },
  });
}
