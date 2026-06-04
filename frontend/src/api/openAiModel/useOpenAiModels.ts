import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface OpenAiModel {
  id: number;
  name: string;
  code: string;
  priceInputStandard: number;
  priceInputCachedStandard: number;
  priceOutputStandard: number;
  priceInputBatch: number;
  priceInputCachedBatch: number;
  priceOutputBatch: number;
  acceptsImageInput: boolean;
}

export function useOpenAiModels() {
  return useQuery({
    queryKey: ["openAiModels"],
    queryFn: async () => {
      const { data } = await axios.get<OpenAiModel[]>(
        "/api/modelos/openai/catalogo/v1/modelos",
      );
      return data;
    },
  });
}
