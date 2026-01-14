import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Prompt } from "./types";

export function useActivatePrompt() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number | string) => {
      const { data } = await axios.post<Prompt>(`/api/prompts/${id}/activate`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["prompts"] });
    },
  });
}
