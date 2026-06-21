import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNichoCnaeV2JobStageStep {
  stageExecutionId: string;
  stageCode: string;
  status: string;
  failureType: string | null;
  attemptNumber: number | null;
  technicalRetryNumber: number | null;
  knowledgeVersion: number | null;
  materializationEnabled: boolean | null;
  inputPayload: string | null;
  outputPayload: string | null;
  errorMessage: string | null;
  nextStageCode: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface OprmNichoCnaeV2JobDetailResponse {
  jobId: string;
  cnaeCode: string;
  status: string;
  finalDecision: string | null;
  finalDecisionLabel: string | null;
  finalDecisionReason: string | null;
  outcomeStatus: "SUCCESS" | "FAILURE" | "IN_PROGRESS" | string | null;
  outcomeMessage: string | null;
  stages: OprmNichoCnaeV2JobStageStep[];
}

async function fetchOprmNichoCnaeV2JobDetail(
  jobId: string,
): Promise<OprmNichoCnaeV2JobDetailResponse> {
  const response = await fetch(
    buildApiUrl(
      `/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions/jobs/${encodeURIComponent(jobId)}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar o detalhe do job v2 (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmNichoCnaeV2JobDetailResponse;
}

export function useOprmNichoCnaeV2JobDetail(jobId: string | undefined) {
  return useQuery({
    queryKey: ["oprm", "nichocnae", "v2", "job-detail", jobId],
    queryFn: () => fetchOprmNichoCnaeV2JobDetail(jobId ?? ""),
    enabled: Boolean(jobId),
  });
}
