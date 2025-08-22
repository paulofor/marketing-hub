import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { PromptEntity } from "./usePromptEntities";

export interface CreatePromptEntity {
  name: string;
}

export function useCreatePromptEntity() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: CreatePromptEntity) => {
      const { data: entity } = await axios.post<PromptEntity>(
        "/api/prompt-entities",
        data,
      );
      return entity;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["promptEntities"] });
    },
  });
}
