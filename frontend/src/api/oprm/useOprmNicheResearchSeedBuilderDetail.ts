import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNicheResearchQueryDetail {
  queryId: number;
  researchCycleId: number;
  nicheResearchSeedId: number;
  queryText: string;
  queryGoal: string;
  sourceGroup: string;
  priority: number;
  status: string;
  resultCount: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface OprmNicheResearchSeedDetail {
  researchCycleId: number;
  nicheResearchSeedId: number;
  cnaeCode: string;
  cnaeDescription: string;
  nicheName: string;
  businessType: string;
  operationType: string;
  customerType: string;
  commercialObjects: string;
  initialAssumptions: string;
  confidenceLevel: string;
  createdBy: string;
  createdAt: string;
  model?: string | null;
  rawModelResponse?: string | null;
  inputTokens?: number | null;
  outputTokens?: number | null;
  costUsd?: number | string | null;
  openAiResponseId?: string | null;
  totalQueries: number;
  queries: OprmNicheResearchQueryDetail[];
}

export interface OprmNicheResearchSeedBuilderDetail {
  researchCycleId: number;
  cycleStatus: string;
  cycleTotalQueries: number;
  cycleErrorMessage?: string | null;
  seed?: OprmNicheResearchSeedDetail | null;
}

async function fetchSeedBuilderDetail(
  researchCycleId: number,
): Promise<OprmNicheResearchSeedBuilderDetail> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/niche-research-seed-builder/stage-executions/${researchCycleId}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar a saída da etapa Seed de Pesquisa do Nicho (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmNicheResearchSeedBuilderDetail;
}

export function useOprmNicheResearchSeedBuilderDetail(
  researchCycleId?: number,
) {
  return useQuery({
    queryKey: [
      "oprm",
      "niche-research-seed-builder",
      "detail",
      researchCycleId,
    ],
    queryFn: () => fetchSeedBuilderDetail(researchCycleId as number),
    enabled: Boolean(researchCycleId),
    retry: false,
  });
}
