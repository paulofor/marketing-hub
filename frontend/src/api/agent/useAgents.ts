import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { Agent } from "./types";

export function useAgents() {
  return useQuery({
    queryKey: ["agents"],
    queryFn: async () => {
      const { data } = await axios.get<Agent[]>("/api/agents");
      return data;
    },
  });
}
