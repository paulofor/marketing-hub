import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmSourceCandidateDetail {
  sourceCandidateId: number;
  researchCycleId: number;
  researchQueryId: number;
  sourceUrl: string;
  sourceTitle: string;
  sourceSnippet?: string | null;
  sourceDomain: string;
  sourceGroup: string;
  searchProvider: string;
  searchPosition: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface OprmSourceSearcherDetail {
  researchCycleId: number;
  cycleStatus: string;
  cycleTotalQueries: number;
  cycleTotalSourceCandidates: number;
  pendingQueries: number;
  completedQueries: number;
  failedQueries: number;
  lastExecutedAt?: string | null;
  lastSearchProvider?: string | null;
  lastErrorMessage?: string | null;
  candidates: OprmSourceCandidateDetail[];
}

async function fetchSourceSearcherDetail(
  researchCycleId: number,
): Promise<OprmSourceSearcherDetail> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/source-searcher/stage-executions/${researchCycleId}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar o resumo da etapa Busca de Fontes (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmSourceSearcherDetail;
}

export function useOprmSourceSearcherDetail(researchCycleId?: number) {
  return useQuery({
    queryKey: ["oprm", "source-searcher", "detail", researchCycleId],
    queryFn: () => fetchSourceSearcherDetail(researchCycleId as number),
    enabled: Boolean(researchCycleId),
    retry: false,
    refetchInterval: 120000,
  });
}
