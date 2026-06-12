import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";
import type { OprmCnaeOpportunityScore } from "./useOprmCnaeOpportunityScores";
import type { OprmTopCnaeMarketVolume } from "./useOprmTopCnaeMarketVolume";

async function fetchCnaeVolume(
  cnaeCode: string,
): Promise<OprmTopCnaeMarketVolume> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/market/import-runs/cnaes/${encodeURIComponent(cnaeCode)}/latest-volume`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar o volume do CNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmTopCnaeMarketVolume;
}

async function fetchCnaeScore(
  cnaeCode: string,
): Promise<OprmCnaeOpportunityScore> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/cnaes/${encodeURIComponent(cnaeCode)}/opportunity-score`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar o score do CNAE (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmCnaeOpportunityScore;
}

export function useOprmCnaeVolume(cnaeCode: string) {
  return useQuery({
    queryKey: ["oprm", "cnaes", cnaeCode, "latest-volume"],
    queryFn: () => fetchCnaeVolume(cnaeCode),
    enabled: Boolean(cnaeCode),
  });
}

export function useOprmCnaeScore(cnaeCode: string) {
  return useQuery({
    queryKey: ["oprm", "cnaes", cnaeCode, "opportunity-score"],
    queryFn: () => fetchCnaeScore(cnaeCode),
    enabled: Boolean(cnaeCode),
    retry: false,
  });
}
