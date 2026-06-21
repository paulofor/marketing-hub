import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNichoCnaeV2JobSummary {
  jobId: string;
  cnaeCode: string;
  status: string;
  currentStageCode: string | null;
  lastStageCode: string | null;
  lastStageStatus: string | null;
  attemptNumber: number | null;
  technicalRetryNumber: number | null;
  knowledgeVersion: number | null;
  materializationEnabled: boolean | null;
  finalDecision: string | null;
  finalDecisionLabel: string | null;
  finalDecisionReason: string | null;
  outcomeStatus: "SUCCESS" | "FAILURE" | "IN_PROGRESS" | string | null;
  outcomeMessage: string | null;
  actionLabel: string | null;
  actionUrl: string | null;
  usedAi: boolean | null;
  aiCostUsd: number | string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface OprmNichoCnaeV2JobsResponse {
  cnaeCode: string;
  cnaeAiCostUsd: number | string | null;
  cnaeUsedAi: boolean | null;
  openJobs: OprmNichoCnaeV2JobSummary[];
  completedJobs: OprmNichoCnaeV2JobSummary[];
}

async function fetchOprmNichoCnaeV2Jobs(
  cnaeCode: string,
): Promise<OprmNichoCnaeV2JobsResponse> {
  const response = await fetch(
    buildApiUrl(
      `/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions/cnaes/${encodeURIComponent(cnaeCode)}/jobs`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os jobs v2 do CNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmNichoCnaeV2JobsResponse;
}

export function useOprmNichoCnaeV2Jobs(cnaeCode: string) {
  return useQuery({
    queryKey: ["oprm", "nichocnae", "v2", cnaeCode, "jobs"],
    queryFn: () => fetchOprmNichoCnaeV2Jobs(cnaeCode),
    enabled: Boolean(cnaeCode),
    refetchInterval: 15000,
  });
}
