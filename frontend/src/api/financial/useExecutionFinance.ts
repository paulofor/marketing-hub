import axios from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { FinancialPlan } from "./useFinancialPlans";

export interface PreparationAuthorization {
  eventId: number;
  reference: string;
  totalLimitUsd: number;
  reserveUsd: number;
  operatorName: string;
  justification: string;
  approvedAt: string;
  productVersion: string;
  current: boolean;
}
export interface ExecutionFinance {
  productId: number;
  productName: string;
  chainId: number;
  cycleId: number;
  experimentId: number;
  productVersion: string;
  revision: number;
  observedAt: string;
  canAuthorize: boolean;
  authorizationBlocker: string | null;
  cycleUrl: string;
  mediaUrl: string;
  videoUrl: string;
  planUrl: string;
  operatorNotesUrl: string;
  guidance: string;
  mediaBudgetBrl: number | null;
  videoBudgetUsd: number | null;
  knownPreparationUsd: number | null;
  unreportedTasks: number;
  costCoverage: string;
  availableUsd: number | null;
  costs: {
    experimentId: number;
    knownUsd: number | null;
    unreportedTasks: number;
  }[];
  authorization: PreparationAuthorization | null;
  history: PreparationAuthorization[];
  financialPlan: FinancialPlan | null;
}
export interface PreparationBudgetDecision {
  requestKey: string;
  expectedRevision: number;
  chainDefinitionId: number;
  experimentId: number;
  productVersion: string;
  totalLimitUsd: number;
  reserveUsd: number;
  operatorName: string;
  justification: string;
  confirmed: boolean;
}
export function useExecutionFinance(
  productId: number,
  chainId: number,
  cycleId: number,
) {
  const client = useQueryClient();
  const root = `/api/business-process-chains/learning-cycles/v1/products/${productId}/${cycleId}`;
  const key = ["execution-finance", productId, chainId, cycleId];
  const query = useQuery({
    queryKey: key,
    enabled: [productId, chainId, cycleId].every(
      (n) => Number.isSafeInteger(n) && n > 0,
    ),
    queryFn: async () =>
      (
        await axios.get<ExecutionFinance>(`${root}/financial-review`, {
          params: { chainId },
        })
      ).data,
    staleTime: 0,
  });
  const approve = useMutation({
    mutationFn: async (decision: PreparationBudgetDecision) =>
      (
        await axios.post<ExecutionFinance>(
          `${root}/preparation-budget`,
          decision,
        )
      ).data,
    onSuccess: (data) => {
      client.setQueryData(key, data);
      void client.invalidateQueries({
        queryKey: ["cycle-process-context", productId],
      });
      void client.invalidateQueries({ queryKey: ["process-automation"] });
    },
  });
  return { query, approve };
}
