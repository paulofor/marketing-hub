import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface AgentWorkMonitor {
  agentId: number;
  agentKey: string;
  nickname: string;
  agentName: string;
  workStatus:
    | "IDLE"
    | "WORKING"
    | "WAITING"
    | "BLOCKED"
    | "DECISION_REQUIRED"
    | "COMPLETED";
  currentWork: string;
  progressDetail?: string | null;
  difficulty?: string | null;
  externalDecisionRequired: boolean;
  externalDecision?: string | null;
  sourceReference?: string | null;
  lastActivityAt?: string | null;
  dailyTokens: number;
  dailyTokenDate: string;
}

/** Consulta o estado persistido das tarefas e pipelines de todos os agentes. */
export function useAgentWorkMonitor() {
  return useQuery({
    queryKey: ["agents", "work-monitor"],
    queryFn: async () =>
      (await axios.get<AgentWorkMonitor[]>("/api/agents/work-monitor")).data,
    refetchInterval: 15_000,
  });
}
