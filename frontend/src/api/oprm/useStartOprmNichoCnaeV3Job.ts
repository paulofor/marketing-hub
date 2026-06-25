import { useMutation } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNichoCnaeV3JobStartResult {
  stageExecutionId: number;
  jobId: string;
  cnaeCode: string;
  stageCode: string;
  status: string;
}

async function startOprmNichoCnaeV3Job(
  cnaeCode: string,
): Promise<OprmNichoCnaeV3JobStartResult> {
  const response = await fetch(
    buildApiUrl(
      `/api/internal/oprm/nichocnae/v3/cnae-intake/stage-executions/cnaes/${encodeURIComponent(cnaeCode)}`,
    ),
    { method: "POST" },
  );
  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      message ||
        `Não foi possível iniciar o job v3 do NichoCNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmNichoCnaeV3JobStartResult;
}

export function useStartOprmNichoCnaeV3Job(cnaeCode: string) {
  return useMutation({
    mutationFn: () => startOprmNichoCnaeV3Job(cnaeCode),
  });
}
