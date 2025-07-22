import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { EmotionalTrigger } from "./useEmotionalTriggers";

export interface CreateEmotionalTrigger {
  name: string;
  valence?: string;
  description?: string;
}

export function useCreateEmotionalTrigger() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreateEmotionalTrigger) => {
      const { data: trigger } = await axios.post<EmotionalTrigger>(
        "/api/emotional-triggers",
        data,
      );
      return trigger;
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["emotionalTriggers"] });
    },
  });
}
