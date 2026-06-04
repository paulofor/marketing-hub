import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { OpenAiModel } from "./useOpenAiModels";
import { OpenAiModelPayload } from "./useCreateOpenAiModel";

export function useUpdateOpenAiModel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: OpenAiModel) => {
      const body: OpenAiModelPayload = {
        name: payload.name,
        code: payload.code,
        priceInputStandard: payload.priceInputStandard,
        priceInputCachedStandard: payload.priceInputCachedStandard,
        priceOutputStandard: payload.priceOutputStandard,
        priceInputBatch: payload.priceInputBatch,
        priceInputCachedBatch: payload.priceInputCachedBatch,
        priceOutputBatch: payload.priceOutputBatch,
        acceptsImageInput: payload.acceptsImageInput,
      };
      const { data } = await axios.put<OpenAiModel>(
        `/api/modelos/openai/catalogo/v1/modelos/${payload.id}`,
        body,
      );
      return data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["openAiModels"] });
      if (data?.id) {
        queryClient.invalidateQueries({
          queryKey: ["openAiModels", data.id.toString()],
        });
      }
    },
  });
}
