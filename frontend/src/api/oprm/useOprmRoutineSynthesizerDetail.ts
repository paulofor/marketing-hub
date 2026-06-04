import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmRoutineCard {
  routineCardId: number;
  researchCycleId: number;
  nicheName: string;
  routineSummary: string;
  painsSummary: string;
  resultsSummary: string;
  mechanismOpportunitiesSummary: string;
  evidenceSummary: string;
  sourceDomains: string;
  confidenceScore: number;
  synthesizedBy: string;
  createdAt: string;
}

export interface OprmRoutineSynthesizerDetail {
  researchCycleId: number;
  cycleStatus: string;
  cycleTotalExtractedSignals: number;
  routineCard: OprmRoutineCard | null;
}

export function useOprmRoutineSynthesizerDetail(researchCycleId?: number) {
  return useQuery({
    queryKey: ["oprm-routine-synthesizer-detail", researchCycleId],
    enabled: Boolean(researchCycleId),
    retry: false,
    refetchInterval: 120000,
    queryFn: async () => {
      const response = await fetch(
        buildApiUrl(
          `/api/oprm/nichocnae/routine-synthesizer/stage-executions/${researchCycleId}`,
        ),
      );
      if (!response.ok) {
        throw new Error(
          `Não foi possível carregar o resumo da etapa Síntese da Rotina (status ${response.status}).`,
        );
      }
      return (await response.json()) as OprmRoutineSynthesizerDetail;
    },
  });
}
