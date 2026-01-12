import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { AgentTheme, AgentThemePayload } from "./types";

export function useUpdateAgentTheme() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, payload }: { id: number; payload: AgentThemePayload }) => {
      const { data } = await axios.put<AgentTheme>(`/api/agent-themes/${id}`, payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["agentThemes"] });
    },
  });
}
