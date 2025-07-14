import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { AiService } from "./useAiServices";

export function useAiService(id: number) {
  return useQuery({
    queryKey: ["aiService", id],
    queryFn: async () => {
      const { data } = await axios.get<AiService>(`/api/ai-services/${id}`);
      return data;
    },
    enabled: !!id,
  });
}
