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

async function fetchTopScores(
  limit = 500,
): Promise<OprmCnaeOpportunityScore[]> {
  const response = await fetch(
    buildApiUrl(`/api/oprm/cnaes/opportunity-scores/top?limit=${limit}`),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os scores de oportunidade dos CNAEs (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmCnaeOpportunityScore[];
}

export function useOprmCnaeOpportunityScores(limit = 500) {
  return useQuery({
    queryKey: ["oprm", "cnaes", "opportunity-scores", "top", limit],
    queryFn: () => fetchTopScores(limit),
  });
}
