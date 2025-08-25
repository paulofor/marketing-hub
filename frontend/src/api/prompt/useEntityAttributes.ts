import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export function useEntityAttributes(entityName: string) {
  return useQuery({
    queryKey: ["entityAttributes", entityName],
    queryFn: async () => {
      const { data } = await axios.get<string[]>(
        `/api/entities/${entityName}/attributes`,
      );
      return data;
    },
    enabled: !!entityName,
  });
}
