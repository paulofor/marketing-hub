import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface UpdateRequest {
  entityName: string;
  description: string;
}

export function useUpdatePromptEntityDescription() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ entityName, description }: UpdateRequest) => {
      const { data } = await axios.put(
        `/api/prompt-entities/${entityName}/description`,
        { description },
      );
      return data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["promptEntities"] });
      queryClient.invalidateQueries({
        queryKey: ["promptEntityDescription", variables.entityName],
      });
    },
  });
}
