import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmGeneratedEnrichedNicheByCnae {
  enrichedNicheProfileId: number;
  marketNicheId: number;
  researchCycleId: number;
  cnaeCode: string;
  cnaeDescription: string;
  nicheName: string;
  qualityStatus: string;
  routineEvidenceScore: number;
  difficultyEvidenceScore: number;
  sourceDiversityScore: number;
  materializedAt: string;
}

async function fetchGeneratedEnrichedNichesByCnae(
  cnaeCode: string,
): Promise<OprmGeneratedEnrichedNicheByCnae[]> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/cnaes/${encodeURIComponent(cnaeCode)}/enriched-niches`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os nichos gerados para este CNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmGeneratedEnrichedNicheByCnae[];
}

export function useOprmGeneratedEnrichedNichesByCnae(cnaeCode: string) {
  return useQuery({
    queryKey: ["oprm", "nichocnae", cnaeCode, "generated-enriched-niches"],
    queryFn: () => fetchGeneratedEnrichedNichesByCnae(cnaeCode),
    enabled: Boolean(cnaeCode),
  });
}
