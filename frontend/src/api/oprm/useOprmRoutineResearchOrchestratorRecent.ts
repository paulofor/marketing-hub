import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export const oprmRoutineResearchOrchestratorRecentQueryKey = (limit = 10) =>
  ["oprm", "routine-research-orchestrator", "recent", limit] as const;

export interface OprmRoutineResearchOrchestratorRecent {
  researchCycleId: number;
  sourceNicheId: number;
  cnaeCode: string;
  cnaeDescription: string;
  nicheName: string;
  originalNicheName?: string | null;
  neutralNicheName?: string | null;
  researchMode?: string | null;
  solutionLanguageRiskScore?: number | null;
  sourceScore: number;
  triggerSource: string;
  cycleStatus: string;
  processedAt: string;
  finishedAt?: string | null;
  errorMessage?: string | null;
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

export interface OprmRoutineResearchOrchestratorReprocessResult {
  researchCycleId: number;
  sourceNicheId: number;
  cnaeCode: string;
  cnaeDescription: string;
  previousCycleStatus: string;
  previousRoutineResearchStatus?: string | null;
  routineResearchStatus: string;
  lastRoutineResearchCycleId?: number | null;
  message: string;
}

async function createNewResearchCycle(
  researchCycleId: number,
): Promise<OprmRoutineResearchOrchestratorReprocessResult> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/routine-research-orchestrator/recent-processed/${researchCycleId}/reprocess`,
    ),
    { method: "POST" },
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível criar novo ciclo de pesquisa para o CNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmRoutineResearchOrchestratorReprocessResult;
}

export function useOprmRoutineResearchOrchestratorRecent(limit = 10) {
  return useQuery({
    queryKey: oprmRoutineResearchOrchestratorRecentQueryKey(limit),
    queryFn: () => fetchRecentProcessed(limit),
  });
}

export function useReprocessOprmRoutineResearchCycle(limit = 10) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createNewResearchCycle,
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: oprmRoutineResearchOrchestratorRecentQueryKey(limit),
      }),
  });
}
