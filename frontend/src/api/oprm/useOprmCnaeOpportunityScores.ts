import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmCnaeOpportunityScore {
  cnaeCode: string;
  cnaeDescription: string;
  opportunityScore: number;
  marketVolumeScore: number;
  meiDensityScore: number;
  digitalFitScore: number;
  painClarityScore: number;
  scoreJustification: string | null;
  algorithmVersion: string;
  cycleId: string;
  scoredAt: string;
  scoreStatus: string;
  enrichedAt: string | null;
}

export type OprmCnaeOpportunityScoreFilter = "all" | "enriched" | "notEnriched";

function buildScoreFilterParam(filter: OprmCnaeOpportunityScoreFilter) {
  if (filter === "enriched") {
    return "&enriched=true";
  }
  if (filter === "notEnriched") {
    return "&notEnriched=true";
  }
  return "";
}

async function fetchTopScores(
  limit = 500,
  filter: OprmCnaeOpportunityScoreFilter = "all",
): Promise<OprmCnaeOpportunityScore[]> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/cnaes/opportunity-scores/top?limit=${limit}${buildScoreFilterParam(filter)}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os scores de oportunidade dos CNAEs (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmCnaeOpportunityScore[];
}

export function useOprmCnaeOpportunityScores(
  limit = 500,
  filter: OprmCnaeOpportunityScoreFilter = "all",
) {
  return useQuery({
    queryKey: ["oprm", "cnaes", "opportunity-scores", "top", limit, filter],
    queryFn: () => fetchTopScores(limit, filter),
  });
}
