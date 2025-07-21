import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface Angle {
  id: number;
  name: string;
  description?: string;
  frameType?: string;
}

export function useAngles() {
  return useQuery({
    queryKey: ["angles"],
    queryFn: async () => {
      const { data } = await axios.get<Angle[]>("/api/angles");
      return data;
    },
  });
}
