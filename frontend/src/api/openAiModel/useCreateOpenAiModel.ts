import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { OpenAiModel } from "./useOpenAiModels";

export type OpenAiModelPayload = Omit<OpenAiModel, "id">;

export function useCreateOpenAiModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: OpenAiModelPayload) => {
      const { data } = await axios.post<OpenAiModel>("/api/openai-models", payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["openAiModels"] });
    },
  });
}
