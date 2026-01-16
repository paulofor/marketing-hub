import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { PromptDomain, PromptDomainPayload } from "./types";

export function useCreatePromptDomain() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: PromptDomainPayload) => {
      const { data } = await axios.post<PromptDomain>("/api/prompt-domains", payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["prompt-domains"] });
    },
  });
}
