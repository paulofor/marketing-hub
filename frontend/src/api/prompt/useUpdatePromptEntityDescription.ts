import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface UpdateRequest {
  entityId: string;
  description: string;
}

export function useUpdatePromptEntityDescription() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ entityId, description }: UpdateRequest) => {
      const { data } = await axios.put(
        `/api/prompt-entities/${entityId}/description`,
        { description },
      );
      return data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["promptEntities"] });
      queryClient.invalidateQueries({
        queryKey: ["promptEntityDescription", variables.entityId],
      });
    },
  });
}
