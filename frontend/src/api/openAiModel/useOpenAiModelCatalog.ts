import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface OpenAiModelCatalog {
  textModels: string[];
  imageModels: string[];
  source: string;
  fetchedAt: string;
}

export function useOpenAiModelCatalog() {
  return useQuery({
    queryKey: ["openAiModelCatalog"],
    queryFn: async () => {
      const { data } = await axios.get<OpenAiModelCatalog>("/api/modelos/openai/catalogo/v1");
      return data;
    },
  });
}
