import { useQuery } from "@tanstack/react-query";
import { buildApiUrl } from "../../utils/buildApiUrl";

export interface OprmCnaeCycle {
  cycleId: string;
  cycleType: string;
  cycleNumber: number;
  status: string;
  selectionCriteria: string | null;
  processedCount: number;
  failedCount: number;
  startedAt: string;
  finishedAt: string | null;
  summary: string | null;
  errorMessage: string | null;
}

async function fetchCycles(limit = 5): Promise<OprmCnaeCycle[]> {
  const response = await fetch(
    buildApiUrl(`/api/oprm/cnae-cycles?limit=${limit}`),
  );
  if (!response.ok) {
    throw new Error(
      `Não foi possível carregar os ciclos CNAE do OPRM (status ${response.status}).`,
    );
  }
  return (await response.json()) as OprmCnaeCycle[];
}

export function useOprmCnaeCycles(limit = 5) {
  return useQuery({
    queryKey: ["oprm", "cnae-cycles", limit],
    queryFn: () => fetchCycles(limit),
  });
}
