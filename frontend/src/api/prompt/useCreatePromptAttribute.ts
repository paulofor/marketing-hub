import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { PromptAttribute } from "./usePromptAttributes";

export interface CreatePromptAttribute {
  name: string;
}

export function useCreatePromptAttribute(entityName: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreatePromptAttribute) => {
      const { data: attr } = await axios.post<PromptAttribute>(
        `/api/prompt-entities/${entityName}/attributes`,
        data,
      );
      return attr;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["promptAttributes", entityName],
      });
      queryClient.invalidateQueries({ queryKey: ["promptEntities"] });
    },
  });
}
