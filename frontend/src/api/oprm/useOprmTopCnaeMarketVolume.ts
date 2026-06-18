import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmTopCnaeMarketVolume {
  snapshotDate: string;
  cnaeCode: string;
  cnaeDescription: string | null;
  totalEstabelecimentos: number;
  totalEstabelecimentosAtivos: number;
  totalEmpresas: number;
  totalEmpresasMei: number;
  totalEmpresasSimples: number;
  opportunityScore: number | null;
  scoreStatus: string | null;
  subnicheCount: number;
  researchCostUsd: number | null;
  nicheResearchRunning: boolean;
}

async function fetchTopCnaes(
  page = 0,
  size = 50,
): Promise<OprmTopCnaeMarketVolume[]> {
  const response = await fetch(
    buildApiUrl(
      `/api/oprm/market/import-runs/cnaes/top-volume?page=${page}&size=${size}`,
    ),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os CNAEs por volume (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmTopCnaeMarketVolume[];
}

export function useOprmTopCnaeMarketVolume(page = 0, size = 50) {
  return useQuery({
    queryKey: ["oprm", "market", "cnaes", "top-volume", page, size],
    queryFn: () => fetchTopCnaes(page, size),
  });
}
