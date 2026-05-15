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
}

async function fetchTopCnaes(limit = 20): Promise<OprmTopCnaeMarketVolume[]> {
  const response = await fetch(buildApiUrl(`/api/oprm/market/import-runs/cnaes/top-volume?limit=${limit}`));
  if (!response.ok) {
    throw new Error(`Não foi possível carregar os CNAEs por volume (status ${response.status}).`);
  }
  return (await response.json()) as OprmTopCnaeMarketVolume[];
}

export function useOprmTopCnaeMarketVolume(limit = 20) {
  return useQuery({
    queryKey: ["oprm", "market", "cnaes", "top-volume", limit],
    queryFn: () => fetchTopCnaes(limit),
  });
}
