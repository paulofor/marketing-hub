import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export type ApolloLearningExperiment = {
  id: number;
  agentKey: string;
  scopeType: string;
  scopeId: string;
  candidateVersion: string;
  baselineVersion: string;
  status: string;
  memoryId: number;
  baselineResultJson?: string;
  candidateResultJson?: string;
  decisionEvidence?: string;
  minimumGain: number;
  maximumCostIncreaseRatio: number;
  regressionPassed: boolean;
  localValidationPassed: boolean;
  createdAt: string;
  evaluatedAt?: string;
  promotedAt?: string;
};

export type ApolloSkillCandidate = {
  id: number;
  experimentId: number;
  skillKey: string;
  baselineVersion: string;
  candidateVersion: string;
  diffSummary: string;
  provenanceJson: string;
  safetyDecision: string;
  safetyEvidence: string;
  status: string;
  monitoredCases: number;
  approvedCases: number;
  rollbackReason?: string;
};

/** Consulta o piloto governado de Apolo sem autorizar promoção, gasto ou publicação. */
export function useApolloLearningExperiments() {
  return useQuery({
    queryKey: ["apollo-learning-experiments"],
    queryFn: async () => {
      const { data } = await axios.get<ApolloLearningExperiment[]>(
        "/api/internal/agent-learning/v1/agents/apollo/experiments",
      );
      return data;
    },
    refetchInterval: 30_000,
  });
}

/** Consulta as skills versionadas do piloto sem executar promoção ou rollback. */
export function useApolloSkillCandidates() {
  return useQuery({
    queryKey: ["apollo-skill-candidates"],
    queryFn: async () => {
      const { data } = await axios.get<ApolloSkillCandidate[]>(
        "/api/internal/agent-learning/v1/agents/apollo/skills",
      );
      return data;
    },
    refetchInterval: 30_000,
  });
}
