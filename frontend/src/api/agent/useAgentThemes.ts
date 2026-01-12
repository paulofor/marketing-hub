import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { AgentTheme } from "./types";

export function useAgentThemes() {
  return useQuery({
    queryKey: ["agentThemes"],
    queryFn: async () => {
      const { data } = await axios.get<AgentTheme[]>("/api/agent-themes");
      return data;
    },
  });
}
