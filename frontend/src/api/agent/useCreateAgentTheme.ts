import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { AgentTheme, AgentThemePayload } from "./types";

export function useCreateAgentTheme() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: AgentThemePayload) => {
      const { data } = await axios.post<AgentTheme>('/api/agent-themes', payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["agentThemes"] });
    },
  });
}
