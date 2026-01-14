import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Prompt, PromptPayload } from "./types";

export function useUpdatePrompt(id: number | string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: PromptPayload) => {
      const { data } = await axios.put<Prompt>(`/api/prompts/${id}`, payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["prompts"] });
      queryClient.invalidateQueries({ queryKey: ["prompt", id] });
    },
  });
}
