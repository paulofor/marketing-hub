import { useMutation } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNichoCnaeV2JobStartResult {
  stageExecutionId: string;
  jobId: string;
  cnaeCode: string;
  sourceNicheId: number;
  attemptNumber: number;
  technicalRetryNumber: number;
  knowledgeVersion: number;
  materializationEnabled: boolean;
  status: string;
}

async function startOprmNichoCnaeV2Job(
  cnaeCode: string,
): Promise<OprmNichoCnaeV2JobStartResult> {
  const response = await fetch(
    buildApiUrl(
      `/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions/cnaes/${encodeURIComponent(cnaeCode)}`,
    ),
    { method: "POST" },
  );
  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      message ||
        `Não foi possível iniciar o job v2 do NichoCNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmNichoCnaeV2JobStartResult;
}

export function useStartOprmNichoCnaeV2Job(cnaeCode: string) {
  return useMutation({
    mutationFn: () => startOprmNichoCnaeV2Job(cnaeCode),
  });
}
