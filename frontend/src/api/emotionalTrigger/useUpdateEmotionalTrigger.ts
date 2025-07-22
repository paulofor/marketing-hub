import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { EmotionalTrigger } from "./useEmotionalTriggers";

export interface UpdateEmotionalTrigger {
  id: number;
  name: string;
  valence?: string;
  description?: string;
}

export function useUpdateEmotionalTrigger() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: async (data: UpdateEmotionalTrigger) => {
      const { data: trigger } = await axios.put<EmotionalTrigger>(
        `/api/emotional-triggers/${data.id}`,
        data,
      );
      return trigger;
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ["emotionalTriggers"] });
    },
  });
}
