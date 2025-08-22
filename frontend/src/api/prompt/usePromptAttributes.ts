import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface PromptAttribute {
  name: string;
  description: string;
  version: number;
}

export function usePromptAttributes(entityName: string) {
  return useQuery({
    queryKey: ["promptAttributes", entityName],
    queryFn: async () => {
      const { data } = await axios.get<PromptAttribute[]>(
        `/api/prompt-entities/${entityName}/attributes`,
      );
      return data;
    },
  });
}
