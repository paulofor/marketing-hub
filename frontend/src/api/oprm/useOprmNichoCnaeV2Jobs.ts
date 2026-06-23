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
  loopDetected: boolean | null;
  loopLabel: string | null;
  loopReason: string | null;
  repeatedStageCount: number | null;
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

function formatReportValue(value: unknown): string {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "string") {
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  }
  return String(value);
}

export async function downloadOprmNichoCnaeV2JobReport(
  job: OprmNichoCnaeV2JobSummary,
) {
  const response = await fetch(
    buildApiUrl(
      `/api/internal/oprm/nichocnae/v2/candidate-generator/stage-executions/jobs/${encodeURIComponent(job.jobId)}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível baixar o relatório do job ${job.jobId} (status ${response.status}).`,
    );
  }
  const detail = (await response.json()) as {
    jobId: string;
    cnaeCode: string;
    outcomeStatus: string | null;
    outcomeMessage: string | null;
    finalDecision: string | null;
    finalDecisionLabel: string | null;
    finalDecisionReason: string | null;
    aiCostUsd: number | string | null;
    stages: Array<{
      stageExecutionId: string;
      stageCode: string;
      status: string;
      inputPayload: string | null;
      outputPayload: string | null;
      errorMessage: string | null;
      nextStageCode: string | null;
      updatedAt: string | null;
    }>;
  };
  const report = [
    `# Relatório do pipeline NichoCNAE v2 — ${detail.jobId}`,
    "",
    `- CNAE: ${detail.cnaeCode}`,
    `- Status: ${detail.outcomeStatus ?? job.status ?? "—"}`,
    `- Decisão: ${detail.finalDecisionLabel ?? detail.finalDecision ?? "—"}`,
    `- Mensagem: ${detail.outcomeMessage ?? detail.finalDecisionReason ?? "—"}`,
    `- Custo IA: ${formatReportValue(detail.aiCostUsd)}`,
    "",
    "## Etapas processadas",
    "",
    ...detail.stages.flatMap((stage, index) => [
      `### ${index + 1}. ${stage.stageCode}`,
      "",
      `- Execução: ${stage.stageExecutionId}`,
      `- Status: ${stage.status}`,
      `- Próxima etapa: ${stage.nextStageCode ?? "—"}`,
      `- Atualizado: ${stage.updatedAt ?? "—"}`,
      `- Falha: ${stage.errorMessage ?? "Sem falha registrada."}`,
      "",
      "#### Entrada",
      "```json",
      formatReportValue(stage.inputPayload),
      "```",
      "",
      "#### Saída / processado",
      "```json",
      formatReportValue(stage.outputPayload),
      "```",
      "",
    ]),
  ].join("\n");
  const blob = new Blob([report], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `oprm-nichocnae-v2-${job.jobId}-relatorio.md`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function useOprmNichoCnaeV2Jobs(cnaeCode: string) {
  return useQuery({
    queryKey: ["oprm", "nichocnae", "v2", cnaeCode, "jobs"],
    queryFn: () => fetchOprmNichoCnaeV2Jobs(cnaeCode),
    enabled: Boolean(cnaeCode),
    refetchInterval: 15000,
  });
}
