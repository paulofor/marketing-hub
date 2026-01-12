import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Agent } from "./types";

export function useAgent(id?: string) {
  return useQuery({
    queryKey: ["agents", id],
    enabled: Boolean(id),
    queryFn: async () => {
      const { data } = await axios.get<Agent>(`/api/agents/${id}`);
      return data;
    },
  });
}
