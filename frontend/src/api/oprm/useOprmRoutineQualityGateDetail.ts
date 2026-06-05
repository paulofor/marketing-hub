import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmRoutineQualityGateDetail {
  researchCycleId: number;
  cycleStatus: string;
  routineCardId: number | null;
  qualityStatus: string | null;
  readyForHypothesis: boolean | null;
  specificityScore: number | null;
  confidenceScore: number | null;
  duplicationScore: number | null;
  qualityNotes: string | null;
  checkedBy: string | null;
  checkedAt: string | null;
}

export function useOprmRoutineQualityGateDetail(researchCycleId?: number) {
  return useQuery({
    queryKey: ["oprm-routine-quality-gate-detail", researchCycleId],
    enabled: Boolean(researchCycleId),
    retry: false,
    refetchInterval: 120000,
    queryFn: async () => {
      const response = await fetch(
        buildApiUrl(
          `/api/oprm/nichocnae/routine-quality-gate/stage-executions/${researchCycleId}`,
        ),
      );
      if (!response.ok) {
        throw new Error(
          `Não foi possível carregar o resumo da etapa Gate de Qualidade (status ${response.status}).`,
        );
      }
      return (await response.json()) as OprmRoutineQualityGateDetail;
    },
  });
}
