import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmExtractedSignalDetail {
  extractedSignalId: number;
  researchCycleId: number;
  sourceSnapshotId: number;
  sourceCandidateId: number;
  signalType: string;
  signalText: string;
  evidenceExcerpt: string;
  sourceDomain: string;
  confidenceScore: number;
  createdBy: string;
  createdAt: string;
}

export interface OprmSignalExtractorDetail {
  researchCycleId: number;
  cycleStatus: string;
  cycleTotalSourceSnapshots: number;
  cycleTotalExtractedSignals: number;
  signals: OprmExtractedSignalDetail[];
}

async function fetchSignalExtractorDetail(
  researchCycleId: number,
): Promise<OprmSignalExtractorDetail> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/signal-extractor/stage-executions/${researchCycleId}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar o resumo da etapa Extração de Sinais (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmSignalExtractorDetail;
}

export function useOprmSignalExtractorDetail(researchCycleId?: number) {
  return useQuery({
    queryKey: ["oprm", "signal-extractor", "detail", researchCycleId],
    queryFn: () => fetchSignalExtractorDetail(researchCycleId as number),
    enabled: Boolean(researchCycleId),
    retry: false,
    refetchInterval: 120000,
  });
}
