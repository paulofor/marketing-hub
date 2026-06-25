import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNichoCnaeV3StageProgress {
  stageExecutionId: number;
  stageCode: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  errorMessage: string | null;
}

export interface OprmNichoCnaeV3JobProgress {
  jobId: string | null;
  cnaeCode: string;
  stages: OprmNichoCnaeV3StageProgress[];
}

async function fetchOprmNichoCnaeV3Progress(
  cnaeCode: string,
): Promise<OprmNichoCnaeV3JobProgress> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/v3/cnaes/${encodeURIComponent(cnaeCode)}/progress`,
    ),
  );

  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      message ||
        `Não foi possível carregar o progresso v3 do NichoCNAE (status ${response.status}).`,
    );
  }

  return (await response.json()) as OprmNichoCnaeV3JobProgress;
}

export function useOprmNichoCnaeV3Progress(cnaeCode: string) {
  return useQuery({
    queryKey: ["oprm-nichocnae-v3-progress", cnaeCode],
    queryFn: () => fetchOprmNichoCnaeV3Progress(cnaeCode),
    enabled: Boolean(cnaeCode),
    refetchInterval: 5000,
  });
}
