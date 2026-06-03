import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmNicheResearchSeedBuilderPending {
  researchCycleId: number;
  sourceNicheId: number;
  cnaeCode: string;
  cnaeDescription: string;
  nicheName: string;
  sourceScore: number;
  triggerSource: string;
  status: string;
  startedAt: string;
  createdAt: string;
}

async function fetchSeedBuilderPending(): Promise<
  OprmNicheResearchSeedBuilderPending[]
> {
  const response = await fetch(
    buildApiUrl(
      "/api/internal/oprm/nichocnae/niche-research-seed-builder/stage-executions/pending",
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível validar a fila da etapa seguinte Seed de Pesquisa do Nicho (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmNicheResearchSeedBuilderPending[];
}

export function useOprmNicheResearchSeedBuilderPending(enabled = true) {
  return useQuery({
    queryKey: ["oprm", "niche-research-seed-builder", "pending"],
    queryFn: fetchSeedBuilderPending,
    enabled,
    retry: false,
  });
}
