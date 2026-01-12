import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { Agent, AgentPayload } from "./types";

export function useCreateAgent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: AgentPayload) => {
      const { data } = await axios.post<Agent>("/api/agents", payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["agents"] });
    },
  });
}
