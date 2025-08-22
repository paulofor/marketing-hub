import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface PromptEntity {
  id: number;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export function usePromptEntities() {
  return useQuery({
    queryKey: ["promptEntities"],
    queryFn: async () => {
      const { data } = await axios.get<PromptEntity[]>("/api/prompt-entities");
      return data;
    },
  });
}
