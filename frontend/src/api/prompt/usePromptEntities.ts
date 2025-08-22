import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export function usePromptEntities() {
  return useQuery({
    queryKey: ["promptEntities"],
    queryFn: async () => {
      const { data } = await axios.get<string[]>("/api/prompt-entities");
      return data;
    },
  });
}
