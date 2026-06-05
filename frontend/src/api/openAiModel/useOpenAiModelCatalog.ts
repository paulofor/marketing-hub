import { useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export interface OpenAiModelCatalogPrice {
  priceInputStandard: number;
  priceInputCachedStandard: number;
  priceOutputStandard: number;
  priceInputBatch: number;
  priceInputCachedBatch: number;
  priceOutputBatch: number;
}

export interface OpenAiModelCatalog {
  textModels: string[];
  imageModels: string[];
  pricingByModel: Record<string, OpenAiModelCatalogPrice>;
  source: string;
  fetchedAt: string;
}

export function useOpenAiModelCatalog() {
  const queryClient = useQueryClient();
  return useQuery({
    queryKey: ["openAiModelCatalog"],
    queryFn: async () => {
      const { data } = await axios.get<OpenAiModelCatalog>(
        "/api/modelos/openai/catalogo/v1",
      );
      queryClient.invalidateQueries({ queryKey: ["openAiModels"] });
      return data;
    },
  });
}
