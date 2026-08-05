import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export interface AgentMaturity {
  agentId: number; agentKey?: string; agentName: string; executions: number;
  completedExecutions: number; failedExecutions: number; openTasks: number;
  resolvedTasks: number; confirmedResults: number; estimatedCost: number;
  completionRate: number; resolutionRate: number; lastExecutionAt?: string;
  maturityLevel: string; nextMaturityAction: string;
}

export function useAgentMaturity() {
  return useQuery({ queryKey: ["agents", "maturity"], queryFn: async () => (await axios.get<AgentMaturity[]>("/api/agents/maturity")).data });
}
