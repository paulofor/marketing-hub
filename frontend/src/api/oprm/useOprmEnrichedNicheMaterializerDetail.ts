import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmEnrichedNicheMaterializerDetail {
  researchCycleId: number;
  cycleStatus: string;
  routineCardId?: number | null;
  marketNicheId?: number | null;
  enrichedNicheProfileId?: number | null;
  nicheName?: string | null;
  cnaeCode?: string | null;
  qualityStatus?: string | null;
  routineSummary?: string | null;
  painsSummary?: string | null;
  resultsSummary?: string | null;
  mechanismOpportunitiesSummary?: string | null;
  evidenceSummary?: string | null;
  sourceDomains?: string | null;
  materializedAt?: string | null;
}

async function fetchOprmEnrichedNicheMaterializerDetail(
  researchCycleId: number,
): Promise<OprmEnrichedNicheMaterializerDetail> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/enriched-niche-materializer/stage-executions/${researchCycleId}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      "Não foi possível consultar a materialização do nicho enriquecido.",
    );
  }
  return (await response.json()) as OprmEnrichedNicheMaterializerDetail;
}

async function fetchOprmEnrichedNicheMaterializerDetailByProfile(
  profileId: number,
): Promise<OprmEnrichedNicheMaterializerDetail> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/nichocnae/enriched-niche-materializer/profiles/${profileId}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      "Não foi possível consultar o perfil do nicho enriquecido.",
    );
  }
  return (await response.json()) as OprmEnrichedNicheMaterializerDetail;
}

export function useOprmEnrichedNicheMaterializerDetail(
  researchCycleId?: number,
) {
  return useQuery({
    queryKey: ["oprm-enriched-niche-materializer-detail", researchCycleId],
    queryFn: () =>
      fetchOprmEnrichedNicheMaterializerDetail(researchCycleId as number),
    enabled: Boolean(researchCycleId),
  });
}

export function useOprmEnrichedNicheMaterializerProfileDetail(
  profileId?: number,
) {
  return useQuery({
    queryKey: ["oprm-enriched-niche-materializer-profile-detail", profileId],
    queryFn: () =>
      fetchOprmEnrichedNicheMaterializerDetailByProfile(profileId as number),
    enabled: Boolean(profileId),
  });
}
