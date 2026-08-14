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
