import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface EmotionalTrigger {
  id: number;
  name: string;
  valence?: string;
  description?: string;
}

export function useEmotionalTriggers() {
  return useQuery({
    queryKey: ["emotionalTriggers"],
    queryFn: async () => {
      const { data } = await axios.get<EmotionalTrigger[]>(
        "/api/emotional-triggers",
      );
      return data;
    },
  });
}
