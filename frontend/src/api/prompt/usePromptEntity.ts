import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { PromptEntity } from "./usePromptEntities";

export function usePromptEntity(entityId: string) {
  return useQuery({
    queryKey: ["promptEntity", entityId],
    queryFn: async () => {
      const { data } = await axios.get<PromptEntity>(
        `/api/prompt-entities/${entityId}`,
      );
      return data;
    },
    enabled: !!entityId,
  });
}
