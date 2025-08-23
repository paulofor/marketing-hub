import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

interface UpdateRequest {
  name: string;
  description: string;
}

export function useUpdatePromptAttribute(entityName: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ name, description }: UpdateRequest) => {
      const { data } = await axios.put(
        `/api/prompt-entities/${entityName}/attributes/${name}`,
        { description },
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["promptAttributes", entityName] });
    },
  });
}
