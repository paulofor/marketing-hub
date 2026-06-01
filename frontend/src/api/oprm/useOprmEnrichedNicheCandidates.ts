import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmEnrichedNicheCandidate {
  id: number;
  cnaeCode: string;
  cnaeDescription: string;
  candidateNicheName: string;
  persona: string | null;
  painHypothesis: string | null;
  desiredOutcome: string | null;
  mechanismHypothesis: string | null;
  proofDirection: string | null;
  offerIdea: string | null;
  marketVolumeSignals: string | null;
  opportunityScore: number;
  scoreCycleId: string;
  enrichmentCycleId: string;
  status: string;
  sourceArtifacts: string | null;
  marketNicheId: number | null;
  createdAt: string;
  updatedAt: string;
}

async function fetchEnrichedNicheCandidates(
  limit = 100,
): Promise<OprmEnrichedNicheCandidate[]> {
  const response = await fetch(
    buildApiUrl(`/api/oprm/cnae-niche-candidates/enriched?limit=${limit}`),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os nichos enriquecidos do OPRM (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmEnrichedNicheCandidate[];
}

export function useOprmEnrichedNicheCandidates(limit = 100, enabled = false) {
  return useQuery({
    queryKey: ["oprm", "cnae-niche-candidates", "enriched", limit],
    queryFn: () => fetchEnrichedNicheCandidates(limit),
    enabled,
  });
}
