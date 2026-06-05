import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { OpenAiModel } from "./useOpenAiModels";

export type OpenAiModelPayload = Omit<OpenAiModel, "id">;
export type CreateOpenAiModelPayload = Pick<OpenAiModel, "name">;

export function useCreateOpenAiModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateOpenAiModelPayload) => {
      const { data } = await axios.post<OpenAiModel>(
        "/api/modelos/openai/catalogo/v1/modelos",
        payload,
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["openAiModels"] });
      queryClient.invalidateQueries({ queryKey: ["openAiModelCatalog"] });
    },
  });
}
