import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import type { AgentDetail } from "./types";

export function useAgentDetail(id?: string) {
  return useQuery({
    queryKey: ["agent-detail", id],
    enabled: Boolean(id),
    queryFn: async () =>
      (await axios.get<AgentDetail>(`/api/agents/${id}/details`)).data,
  });
}
