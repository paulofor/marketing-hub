import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { PromptDomain, PromptDomainPayload } from "./types";

export function useUpdatePromptDomain(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: PromptDomainPayload) => {
      const { data } = await axios.put<PromptDomain>(`/api/prompt-domains/${id}`, payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["prompt-domains"] });
      queryClient.invalidateQueries({ queryKey: ["prompt-domain", id] });
    },
  });
}
