import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Agent, AgentPayload } from "./types";

export function useUpdateAgent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, payload }: { id: number; payload: AgentPayload }) => {
      const { data } = await axios.put<Agent>(`/api/agents/${id}`, payload);
      return data;
    },
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["agents"] });
      queryClient.invalidateQueries({ queryKey: ["agents", String(variables.id)] });
    },
  });
}
