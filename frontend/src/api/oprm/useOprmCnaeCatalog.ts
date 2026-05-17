import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmCnaeCatalogItem {
  cnaeCode: string;
  description: string;
  active: boolean;
}

async function fetchCnaeCatalog(): Promise<OprmCnaeCatalogItem[]> {
  const response = await fetch(buildApiUrl("/api/oprm/market/import-runs/cnaes"));
  if (!response.ok) {
    throw new Error(`Não foi possível carregar o catálogo de CNAEs (status ${response.status}).`);
  }
  return (await response.json()) as OprmCnaeCatalogItem[];
}

export function useOprmCnaeCatalog() {
  return useQuery({
    queryKey: ["oprm", "market", "cnaes", "catalog"],
    queryFn: fetchCnaeCatalog,
  });
}
