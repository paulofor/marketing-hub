import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface PromptEntityDescription {
  description: string;
}

export function usePromptEntityDescription(entityId: string) {
  return useQuery({
    queryKey: ["promptEntityDescription", entityId],
    queryFn: async () => {
      const { data } = await axios.get<PromptEntityDescription>(
        `/api/prompt-entities/${entityId}/description`,
      );
      return data;
    },
  });
}
