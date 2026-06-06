import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmSourceSnapshotDetail {
  sourceSnapshotId: number;
  researchCycleId: number;
  sourceCandidateId: number;
  sourceUrl: string;
  sourceDomain: string;
  sourceTitle: string;
  sourceType: string;
  sourceIntent?: string | null;
  routineEvidenceScore?: number | null;
  commercialPageRisk?: boolean | null;
  solutionLanguageRisk?: boolean | null;
  snippet?: string | null;
  shortExcerpt: string;
  fetchedAt: string;
  fetchStatus: string;
  httpStatus?: number | null;
  storagePolicy: string;
  licenseState?: string | null;
  errorMessage?: string | null;
  createdAt: string;
}

export interface OprmSourceFetcherDetail {
  researchCycleId: number;
  cycleStatus: string;
  cycleTotalSourceCandidates: number;
  cycleTotalSourceSnapshots: number;
  snapshots: OprmSourceSnapshotDetail[];
}

async function fetchSourceFetcherDetail(
  researchCycleId: number,
): Promise<OprmSourceFetcherDetail> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/source-fetcher/stage-executions/${researchCycleId}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar o resumo da etapa Coleta de Fontes (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmSourceFetcherDetail;
}

export function useOprmSourceFetcherDetail(researchCycleId?: number) {
  return useQuery({
    queryKey: ["oprm", "source-fetcher", "detail", researchCycleId],
    queryFn: () => fetchSourceFetcherDetail(researchCycleId as number),
    enabled: Boolean(researchCycleId),
    retry: false,
    refetchInterval: 120000,
  });
}
