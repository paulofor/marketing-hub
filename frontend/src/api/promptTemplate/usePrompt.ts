import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Prompt } from "./types";

export function usePrompt(id?: string | number) {
  return useQuery({
    queryKey: ["prompt", id],
    enabled: Boolean(id),
    queryFn: async () => {
      const { data } = await axios.get<Prompt>(`/api/prompts/${id}`);
      return data;
    },
  });
}
