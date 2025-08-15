import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface AiService {
  id: number;
  name: string;
  objective: string;
  url: string;
  phase: string;
  price: number;
  cost: number;
  observation: string;
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
