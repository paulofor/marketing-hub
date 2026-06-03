import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmRoutineResearchOrchestratorRecent {
  researchCycleId: number;
  sourceNicheId: number;
  cnaeCode: string;
  cnaeDescription: string;
  nicheName: string;
  sourceScore: number;
  triggerSource: string;
  cycleStatus: string;
  processedAt: string;
}

async function fetchRecentProcessed(
  limit = 10,
): Promise<OprmRoutineResearchOrchestratorRecent[]> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/routine-research-orchestrator/recent-processed?limit=${limit}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os nichos processados pelo orquestrador (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmRoutineResearchOrchestratorRecent[];
}

export function useOprmRoutineResearchOrchestratorRecent(limit = 10) {
  return useQuery({
    queryKey: ["oprm", "routine-research-orchestrator", "recent", limit],
    queryFn: () => fetchRecentProcessed(limit),
  });
}
