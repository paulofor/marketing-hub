import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface PromptAttribute {
  name: string;
  description: string;
  version: number;
}

export function usePromptAttributes(entityId: string) {
  return useQuery({
    queryKey: ["promptAttributes", entityId],
    queryFn: async () => {
      const { data } = await axios.get<PromptAttribute[]>(
        `/api/prompt-entities/${entityId}/attributes`,
      );
      return data;
    },
  });
}
