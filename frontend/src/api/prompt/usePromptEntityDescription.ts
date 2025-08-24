import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface PromptEntityDescription {
  description: string;
  version: number;
}

export function usePromptEntityDescription(entityName: string) {
  return useQuery({
    queryKey: ["promptEntityDescription", entityName],
    queryFn: async () => {
      const { data } = await axios.get<PromptEntityDescription>(
        `/api/prompt-entities/${entityName}/description`,
      );
      return data;
    },
  });
}
