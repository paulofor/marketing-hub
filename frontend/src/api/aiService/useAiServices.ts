import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface AiService {
  id: number;
  name: string;
  objective: string;
  price: number;
  cost: number;
}

export function useAiServices() {
  return useQuery({
    queryKey: ["aiServices"],
    queryFn: async () => {
      const { data } = await axios.get<AiService[]>("/api/ai-services");
      return data;
    },
  });
}
