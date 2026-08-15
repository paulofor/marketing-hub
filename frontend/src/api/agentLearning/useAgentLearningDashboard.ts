import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

export type AgentLearningSummary = {
  agentKey: string;
  agentName: string;
  totalMemories: number;
  candidateMemories: number;
  confirmedMemories: number;
  contradictedMemories: number;
  retiredMemories: number;
  totalRetrievals: number;
};

export type AgentLearningMemory = {
  id: number;
  agentKey: string;
  agentName: string;
  tenantKey: string;
  scopeType: string;
  scopeId: string;
  specialty: string;
  content: string;
  evidence: string;
  sourceReference?: string;
  sourceExecutionId: string;
  status: string;
  confidence: number;
  retrievalCount: number;
  lastRetrievedAt?: string;
  validUntil?: string;
  createdAt: string;
  updatedAt: string;
};

export type AgentLearningDashboard = {
  totalMemories: number;
  candidateMemories: number;
  confirmedMemories: number;
  contradictedMemories: number;
  retiredMemories: number;
  totalRetrievals: number;
  agents: AgentLearningSummary[];
  memories: AgentLearningMemory[];
};

/** Consulta somente evidências persistidas de aprendizado e reutilização. */
export function useAgentLearningDashboard() {
  return useQuery({
    queryKey: ["agent-learning-dashboard"],
    queryFn: async () => {
      const { data } = await axios.get<AgentLearningDashboard>(
        "/api/agent-learning-dashboard/v1",
      );
      return data;
    },
  });
}

/** Registra uma decisão humana com evidência na trilha append-only. */
export function useReviewAgentMemory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (input: {
      agentKey: string;
      memoryId: number;
      outcome: "CONFIRMED" | "CONTRADICTED" | "RETIRED";
      evidence: string;
    }) => {
      await axios.post(
        `/api/agent-learning-dashboard/v1/agents/${input.agentKey}/memories/${input.memoryId}/feedback`,
        { outcome: input.outcome, evidence: input.evidence },
      );
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["agent-learning-dashboard"] }),
  });
}
