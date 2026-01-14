import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Prompt, PromptPayload } from "./types";

export function useCreatePrompt() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: PromptPayload) => {
      const { data } = await axios.post<Prompt>("/api/prompts", payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["prompts"] });
    },
  });
}
