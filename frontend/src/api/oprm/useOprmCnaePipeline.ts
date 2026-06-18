import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmRoutineResearchCycleSummary {
  researchCycleId: number;
  sourceNicheId: number;
  cnaeCode: string;
  nicheName: string;
  originalNicheName: string | null;
  neutralNicheName: string | null;
  researchMode: string;
  solutionLanguageRiskScore: number | null;
  sourceScore: number | null;
  triggerSource: string | null;
  status: string;
  currentStageCode?: string | null;
  totalQueries: number | null;
  totalSourceCandidates: number | null;
  totalSourceSnapshots: number | null;
  totalExtractedSignals: number | null;
  executionCostUsd: number | string | null;
  cnaeTotalCostUsd: number | string | null;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string | null;
}

export interface OprmRoutineResearchStartResult {
  started: boolean;
  researchCycleId: number | null;
  sourceNicheId: number | null;
  cnaeCode: string | null;
  cnaeDescription: string | null;
  nicheName: string | null;
  sourceScore: number | null;
  triggerSource: string | null;
  cycleStatus: string | null;
  originalNicheName: string | null;
  neutralNicheName: string | null;
  researchMode: string | null;
  solutionLanguageRiskScore: number | null;
  routineResearchStatus: string | null;
  message: string | null;
}

async function fetchCnaeCycles(
  cnaeCode: string,
): Promise<OprmRoutineResearchCycleSummary[]> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/cnaes/${encodeURIComponent(cnaeCode)}/routine-research-cycle/stage-executions`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os ciclos NichoCNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmRoutineResearchCycleSummary[];
}

async function startCnaePipeline(
  cnaeCode: string,
): Promise<OprmRoutineResearchStartResult> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/cnaes/${encodeURIComponent(cnaeCode)}/routine-research-orchestrator/run`,
    ),
    { method: "POST" },
  );
  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      message ||
        `Não foi possível disparar o pipeline NichoCNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmRoutineResearchStartResult;
}

export async function downloadOprmCnaeCycleReport(researchCycleId: number) {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/routine-research-cycle/stage-executions/${researchCycleId}/report`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível baixar o relatório da execução (status ${response.status}).`,
    );
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `nicho-cnae${researchCycleId}.md`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function useOprmCnaePipelineCycles(cnaeCode: string) {
  return useQuery({
    queryKey: ["oprm", "nichocnae", cnaeCode, "routine-cycles"],
    queryFn: () => fetchCnaeCycles(cnaeCode),
    enabled: Boolean(cnaeCode),
    refetchInterval: 15000,
  });
}

export function useStartOprmCnaePipeline(cnaeCode: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => startCnaePipeline(cnaeCode),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ["oprm", "nichocnae", cnaeCode, "routine-cycles"],
      });
    },
  });
}
