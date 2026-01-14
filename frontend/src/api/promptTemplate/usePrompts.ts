import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Prompt } from "./types";

export function usePrompts(domain?: string) {
  return useQuery({
    queryKey: ["prompts", domain ?? "all"],
    queryFn: async () => {
      const params = domain ? { domain } : undefined;
      const { data } = await axios.get<Prompt[]>("/api/prompts", { params });
      return data;
    },
  });
}
