import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface DifferentiatedTechnology {
  id: number;
  name: string;
  description?: string | null;
  promptText?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export function useDifferentiatedTechnologies() {
  return useQuery({
    queryKey: ["differentiatedTechnologies"],
    queryFn: async () => {
      const { data } = await axios.get<DifferentiatedTechnology[]>(
        "/api/differentiated-technologies",
      );
      return data;
    },
  });
}
