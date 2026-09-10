import axios from "axios";
import { useQuery } from "@tanstack/react-query";
import { cycleApi } from "./useLearningCycles";

export type CycleProcessContext = {
  cycleId: number;
  cycleNumber: number;
  experimentId: number;
  chainDefinitionId: number;
  productVersion: string;
  status: string;
  stageLabel: string;
  hypothesis: string;
  mainChange: string;
  cycleUrl: string;
  previousLearning: {
    cycleId: number;
    experimentId: number;
    action: string;
    summary: string;
    evidenceReference: string;
    learning: string;
    nextHypothesis: string;
    limitation: string;
  }[];
  nextWork: {
    processDefinitionId: number;
    processNumber: number;
    processName: string;
    activityId: string;
    activityNumber: number;
    activityName: string;
    responsible: string;
    state: string;
    reason: string;
    url: string;
  } | null;
};

/** Consulta a identidade e a continuidade oficiais, inclusive na entrada sem parâmetro de ciclo. */
export function useCycleProcessContext(
  productId?: number,
  processDefinitionId?: number,
  cycleId?: number,
  chainId?: number,
) {
  return useQuery({
    queryKey: [
      "cycle-process-context",
      productId,
      processDefinitionId,
      cycleId,
      chainId,
    ],
    enabled: Boolean(productId && processDefinitionId),
    queryFn: async () =>
      (
        await axios.get<CycleProcessContext | null>(
          `${cycleApi}/products/${productId}/process-context`,
          {
            params: { processDefinitionId, cycleId, chainId },
          },
        )
      ).data || null,
  });
}
