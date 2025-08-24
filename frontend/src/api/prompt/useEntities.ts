import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export function useEntities() {
  return useQuery({
    queryKey: ["entities"],
    queryFn: async () => {
      const { data } = await axios.get<string[]>("/api/entities");
      return data;
    },
  });
}
