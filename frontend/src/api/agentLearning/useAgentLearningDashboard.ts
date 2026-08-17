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

export type TemisVisualLearningRun = {
  id: number;
  contextKey: string;
  status: string;
  baselineVersion: string;
  candidateVersion: string;
  memoryId?: number;
  learningExperimentId?: number;
  error?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt: string;
};

export type TemisVisualLearningMetric = {
  contextKey: string;
  playbookVersion: string;
  cases: number;
  firstPassApprovalRate: number;
  approvalWithinThreeRate: number;
  recurringIssueRate: number;
  averageCostPerApprovedAsset: number;
  minimumPremiumScore: number;
};

export type TemisVisualLearningBackfill = {
  experimentId: number;
  scannedAssets: number;
  scannedCreatives: number;
  ingestedCases: number;
  generatedRuns: number;
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

/** Consulta as consolidações visuais auditáveis de Têmis. */
export function useTemisVisualLearningRuns() {
  return useQuery({
    queryKey: ["temis-visual-learning-runs"],
    queryFn: async () => {
      const { data } = await axios.get<TemisVisualLearningRun[]>(
        "/api/internal/agent-learning/v1/agents/meta-ad-approver/visual-learning/runs",
      );
      return data;
    },
  });
}

/** Consulta métricas reais por contexto e versão de playbook. */
export function useTemisVisualLearningMetrics() {
  return useQuery({
    queryKey: ["temis-visual-learning-metrics"],
    queryFn: async () => {
      const { data } = await axios.get<TemisVisualLearningMetric[]>(
        "/api/internal/agent-learning/v1/agents/meta-ad-approver/visual-learning/metrics",
      );
      return data;
    },
  });
}

/** Promove uma candidata somente após a decisão humana explícita no painel. */
export function usePromoteTemisVisualLearningRun() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (runId: number) => {
      await axios.post(
        `/api/internal/agent-learning/v1/agents/meta-ad-approver/visual-learning/runs/${runId}/promotion`,
      );
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["temis-visual-learning-runs"],
        }),
        queryClient.invalidateQueries({
          queryKey: ["temis-visual-learning-metrics"],
        }),
        queryClient.invalidateQueries({
          queryKey: ["agent-learning-dashboard"],
        }),
      ]);
    },
  });
}

/** Incorpora pareceres históricos sem reexecutar provider, geração ou aprovação. */
export function useBackfillTemisVisualLearningHistory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (experimentId: number) => {
      const { data } = await axios.post<TemisVisualLearningBackfill>(
        `/api/internal/agent-learning/v1/agents/meta-ad-approver/visual-learning/history/${experimentId}/backfill`,
      );
      return data;
    },
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["temis-visual-learning-runs"],
        }),
        queryClient.invalidateQueries({
          queryKey: ["temis-visual-learning-metrics"],
        }),
      ]);
    },
  });
}
