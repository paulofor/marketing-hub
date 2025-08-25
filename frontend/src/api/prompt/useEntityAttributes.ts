import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export function useEntityAttributes(entityId: string) {
  return useQuery({
    queryKey: ["entityAttributes", entityId],
    queryFn: async () => {
      const { data } = await axios.get<string[]>(`/api/entities/${entityId}/attributes`);
      return data;
    },
  });
}
