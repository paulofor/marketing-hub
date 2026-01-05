import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { OpenAiModel } from "./useOpenAiModels";

export function useOpenAiModel(id?: string) {
  return useQuery({
    queryKey: ["openAiModels", id],
    enabled: Boolean(id),
    queryFn: async () => {
      const { data } = await axios.get<OpenAiModel>(`/api/openai-models/${id}`);
      return data;
    },
  });
}
