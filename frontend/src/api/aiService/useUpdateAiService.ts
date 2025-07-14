import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { AiService } from "./useAiServices";

export function useUpdateAiService() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (data: AiService) => {
      const { data: service } = await axios.put<AiService>(
        `/api/ai-services/${data.id}`,
        data,
      );
      return service;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["aiServices"] });
    },
  });
}
